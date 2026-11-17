package dev.lapis256.hauyne.buildlogic.release.prepare

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PrepareReleaseTest {
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
            assertEquals("v$version\n", root.resolve("build/release/tag").readText())
            assertEquals("modVersion=$version\n", root.resolve("gradle.properties").readText())
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
            assertEquals("v$version\n", root.resolve("build/release/tag").readText())
            assertEquals("modVersion=$version\n", root.resolve("gradle.properties").readText())
            val notes = root.resolve("build/release/notes.md").readText()
            val full = root.resolve("CHANGELOG.md").readText()
            assertTrue(message.substringAfter(": ") in notes.lowercase())
            assertTrue("initial" !in notes.lowercase() && "initial" in full.lowercase())
            assertTrue(notes.trim() in full)
        }
        root.resolve("gradle.properties").writeText("modVersion=0.2.0\n")
        assertThrows(IllegalArgumentException::class.java) { task.prepare() }
    }

    @Test
    fun taggedReleaseNotesMatchPreparedNotes() {
        val task = prepareTask()
        git("tag", "v0.1.0")
        git("commit", "--allow-empty", "-qm", "fix: repaired")
        task.prepare()
        val notes = root.resolve("build/release/notes.md").readText()
        git("add", "gradle.properties", "CHANGELOG.md")
        git("commit", "-qm", "chore(release): v0.1.1")
        git("tag", "v0.1.1")
        val process = ProcessBuilder(
            "git-cliff",
            "--offline",
            "--use-branch-tags",
            "--config",
            "cliff.toml",
            "--current",
            "--strip",
            "header",
        ).directory(root).start()
        val taggedNotes = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor())
        assertEquals(notes.trim(), taggedNotes.trim())
    }
}
