import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReleaseTest {
    @TempDir
    lateinit var root: File

    private fun git(vararg args: String) {
        val process = ProcessBuilder("git", "-c", "commit.gpgsign=false", *args)
            .directory(root).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { output }
    }

    private fun prepareTask(): PrepareRelease {
        File("../cliff.toml").copyTo(root.resolve("cliff.toml"))
        root.resolve("gradle.properties").writeText("modVersion=0.1.0\n")
        git("init", "--quiet")
        git("config", "user.name", "Test")
        git("config", "user.email", "test@example.com")
        git("add", "cliff.toml", "gradle.properties")
        git("commit", "-qm", "chore: initial")
        return ProjectBuilder.builder().withProjectDir(root).build().tasks.register("prepare", PrepareRelease::class.java).get().apply {
            rootDirectory.set(root)
            bump.set("auto")
        }
    }

    @Test
    fun initialReleaseSupportsEveryBump() {
        val task = prepareTask()
        for ((mode, version) in listOf("auto" to "0.1.0", "major" to "1.0.0", "minor" to "0.2.0", "patch" to "0.1.1")) {
            root.resolve("gradle.properties").writeText("modVersion=0.1.0\n")
            task.bump.set(mode)
            task.prepare()
            assertEquals("v$version\n", root.resolve(".release/tag").readText())
        }
    }

    @Test
    fun autoBumpsAndSeparatesReleaseNotesFromHistory() {
        val task = prepareTask()
        git("tag", "v0.1.0")
        assertThrows(IllegalArgumentException::class.java) { task.prepare() }
        for ((message, version) in listOf("fix: repaired" to "0.1.1", "feat: added" to "0.2.0", "feat!: replaced" to "1.0.0")) {
            root.resolve("gradle.properties").writeText("modVersion=0.1.0\n")
            git("commit", "--allow-empty", "-qm", message)
            task.prepare()
            assertEquals("v$version\n", root.resolve(".release/tag").readText())
            val notes = root.resolve(".release/notes.md").readText()
            val full = root.resolve("CHANGELOG.md").readText()
            assertTrue(message.substringAfter(": ") in notes.lowercase())
            assertTrue("initial" !in notes.lowercase() && "initial" in full.lowercase())
            assertTrue(notes.trim() in full)
        }
        root.resolve("gradle.properties").writeText("modVersion=0.2.0\n")
        assertThrows(IllegalArgumentException::class.java) { task.prepare() }
    }

    @Test
    fun integrityRejectsChangedMissingAndUnrecordedFiles() {
        val artifact = root.resolve("artifact.jar").apply { writeText("verified") }
        val manifest = root.resolve("SHA256SUMS").apply {
            writeText("${ReleaseArtifacts.sha256(artifact)}  artifact.jar\n")
        }
        ReleaseArtifacts.verify(root, manifest, listOf(artifact))
        artifact.writeText("changed")
        assertThrows(IllegalArgumentException::class.java) { ReleaseArtifacts.verify(root, manifest) }
        artifact.writeText("verified")
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseArtifacts.verify(root, manifest, listOf(root.resolve("unrecorded.jar")))
        }
        artifact.delete()
        assertThrows(java.io.FileNotFoundException::class.java) { ReleaseArtifacts.verify(root, manifest) }
    }

    @Test
    fun settingsValidationCanOnlyBeSkippedWhenNotRequired() {
        val task = ProjectBuilder.builder().withProjectDir(root).build().tasks
            .register("validate", ValidateReleaseSettings::class.java).get()
        task.configurationPresent.put("token", false)
        task.required.set(false)
        task.validate()
        task.required.set(true)
        assertThrows(IllegalArgumentException::class.java) { task.validate() }
        task.configurationPresent.put("token", true)
        task.validate()
    }

    @Test
    fun recordsAnyRegisteredLoaderAndRejectsUnregisteredModFiles() {
        root.resolve(".release").mkdirs()
        for (path in listOf("gradle.properties", "CHANGELOG.md", ".release/notes.md")) root.resolve(path).writeText("metadata")
        val source = "future-loader/build/libs/future-loader.jar"
        val target = "dev/lapis256/future-loader/1.0.0/future-loader-1.0.0.jar"
        val mod = root.resolve(source).apply {
            parentFile.mkdirs()
            writeText("verified mod")
        }
        val repository = root.resolve("maven")
        repository.resolve(target).apply {
            parentFile.mkdirs()
            writeText("verified mod")
        }
        val task = ProjectBuilder.builder().withProjectDir(root).build().tasks
            .register("record", RecordReleaseArtifacts::class.java).get().apply {
                rootDirectory.set(root)
                localRepository.set(repository)
                publicationFiles.put(source, target)
                modFiles.from(mod)
            }
        task.record()
        assertEquals("verified mod", root.resolve(".release/assets/future-loader-1.0.0.jar").readText())
        val manifest = root.resolve(".release/SHA256SUMS")
        ReleaseArtifacts.verify(root, manifest, listOf(mod))
        val otherMod = root.resolve("other.jar").apply { writeText("unregistered") }
        task.modFiles.setFrom(otherMod)
        val error = assertThrows(IllegalArgumentException::class.java) { task.record() }
        assertTrue(error.message!!.contains("not a Maven publication"))
    }
}
