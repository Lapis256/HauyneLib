package dev.lapis256.hauyne.buildlogic.release.r2

import dev.lapis256.hauyne.buildlogic.release.staging.ReleaseArtifacts
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Verifies the current published JAR bytes")
abstract class VerifyPublishedMaven : R2Task() {
    @TaskAction
    fun verify() = R2Results(resultDirectory.get().asFile, "r2-verify").run {
        val manifest = ReleaseArtifacts.readManifest(stagingDirectory.get().asFile)
        val jars = manifest.filterKeys { it.endsWith(".jar") }
        require(jars.isNotEmpty()) { "No staged JARs to verify" }
        jars.keys.forEach(::requireMavenPath)
        createS3Client().use { client ->
            for ((key, expected) in jars) {
                val matches = readRemote(client, bucket.get(), key)?.sha256 == expected
                record(key, if (matches) "verified" else "mismatch", expected)
                require(matches) { "Published JAR missing or changed: $key" }
            }
        }
    }
}
