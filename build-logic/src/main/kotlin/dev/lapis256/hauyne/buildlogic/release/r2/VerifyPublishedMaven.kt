package dev.lapis256.hauyne.buildlogic.release.r2

import dev.lapis256.hauyne.buildlogic.release.staging.ReleaseArtifacts
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Verifies the current published JAR bytes")
abstract class VerifyPublishedMaven : R2Task() {
    @TaskAction
    fun verify() {
        val manifest = ReleaseArtifacts.readManifest(stagingDirectory.get().asFile)
        val jars = manifest.filterKeys { it.endsWith(".jar") }
        require(jars.isNotEmpty()) { "No staged JARs to verify" }
        jars.keys.forEach(::requireMavenPath)
        createS3Client().use { client ->
            for ((key, expected) in jars) {
                val matches = remoteSha256(client, bucket.get(), key) == expected
                require(matches) { "Published JAR missing or changed: $key" }
            }
        }
    }
}
