package dev.lapis256.hauyne.buildlogic.release.staging

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class StagedReleaseTest {
    @TempDir
    lateinit var root: File

    @Test
    fun integrityRejectsChangedMissingAndUnrecordedFiles() {
        val task = stagingTask()
        task.record()
        val staging = root.resolve("staging")
        val artifact = staging.resolve("maven/dev/example/lib/1.0/lib-1.0.jar")
        assertTrue("dev/example/lib/1.0/lib-1.0.jar" in ReleaseArtifacts.verifyStaging(staging))
        artifact.writeText("changed")
        assertThrows(IllegalArgumentException::class.java) { ReleaseArtifacts.verifyStaging(staging) }
        artifact.writeText("verified")
        val extra = staging.resolve("maven/unrecorded.jar").apply { writeText("unrecorded") }
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseArtifacts.verifyStaging(staging)
        }
        extra.delete()
        artifact.delete()
        assertThrows(IllegalArgumentException::class.java) { ReleaseArtifacts.verifyStaging(staging) }
    }

    @Test
    fun rejectsMismatchedMavenPublication() {
        val task = stagingTask()
        root.resolve("staging/maven/dev/example/lib/1.0/lib-1.0.jar").writeText("different")
        val error = assertThrows(IllegalArgumentException::class.java) { task.record() }
        assertTrue(error.message!!.contains("Staged Maven publication differs"))
    }

    @Test
    fun sealingRejectsRestagingDuplicateEntriesAndUnsafePaths() {
        val task = stagingTask()
        task.record()
        assertThrows(IllegalArgumentException::class.java) { task.record() }
        val staging = root.resolve("staging")
        val manifest = staging.resolve("SHA256SUMS")
        val original = manifest.readText()
        manifest.appendText(original.lineSequence().first() + "\n")
        assertThrows(IllegalArgumentException::class.java) { ReleaseArtifacts.verifyStaging(staging) }
        manifest.writeText(original)
        for (path in listOf("../outside.jar", "/absolute.jar", "C:/outside.jar", "maven/../outside.jar", "maven\\outside.jar")) {
            manifest.writeText(original + "${"0".repeat(64)}  $path\n")
            assertThrows(IllegalArgumentException::class.java) { ReleaseArtifacts.verifyStaging(staging) }
        }
    }

    private fun stagingTask(): RecordReleaseArtifacts {
        root.resolve("artifact.jar").writeText("verified")
        val staging = root.resolve("staging")
        staging.resolve("maven/dev/example/lib/1.0").mkdirs()
        staging.resolve("maven/dev/example/lib/1.0/lib-1.0.jar").writeText("verified")
        staging.resolve("maven/dev/example/lib/maven-metadata.xml").writeText(
            "<metadata><versioning><versions><version>1.0</version></versions></versioning></metadata>",
        )
        staging.resolve("baseline-metadata.json").writeText("{}\n")
        return ProjectBuilder.builder().withProjectDir(root).build().tasks
            .register("record", RecordReleaseArtifacts::class.java).get().apply {
                rootDirectory.set(root)
                stagingDirectory.set(staging)
                publicationFiles.put("artifact.jar", "dev/example/lib/1.0/lib-1.0.jar")
            }
    }
}
