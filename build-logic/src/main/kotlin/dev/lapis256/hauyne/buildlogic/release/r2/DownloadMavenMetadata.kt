package dev.lapis256.hauyne.buildlogic.release.r2

import dev.lapis256.hauyne.buildlogic.release.staging.MavenMetadata
import kotlinx.serialization.json.JsonObject
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import software.amazon.awssdk.services.s3.model.GetObjectRequest

@UntrackedTask(because = "Reads the current remote metadata and ETags")
abstract class DownloadMavenMetadata : R2Task() {
    @get:Internal
    abstract val coordinates: ListProperty<String>

    @TaskAction
    fun download() {
        val staging = stagingDirectory.get().asFile
        require(!staging.resolve("SHA256SUMS").exists()) { "Cannot modify sealed staging" }
        val modules = coordinates.get().distinct().sorted()
        require(modules.isNotEmpty()) { "No Maven coordinates registered" }
        modules.forEach {
            requireMavenPath(it)
            require('/' in it) { "Expected group-path/artifact: $it" }
        }
        val baseline = mutableMapOf<String, JsonObject>()
        val baselineFile = staging.resolve("baseline-metadata.json")
        // 途中失敗時に古い基準情報で公開されることを防ぐ。
        if (baselineFile.exists()) check(baselineFile.delete()) { "Cannot invalidate metadata baseline" }
        createS3Client().use { client ->
            for (module in modules) {
                for (suffix in metadataSuffixes) {
                    val key = "$module/maven-metadata.xml$suffix"
                    val target = staging.resolve("maven/$key")
                    val etag = missingOn404 {
                        client.getObject(GetObjectRequest.builder().bucket(bucket.get()).key(key).build()).use { input ->
                            val value = requireNotNull(input.response().eTag()) { "Missing ETag: $key" }
                            if (suffix.isEmpty()) {
                                target.parentFile.mkdirs()
                                target.outputStream().use { input.copyTo(it) }
                            }
                            value
                        }
                    }
                    if (etag == null) {
                        if (target.exists()) check(target.delete()) { "Cannot remove stale metadata: $key" }
                    }
                    baseline[key] = MavenMetadata.entry(etag, if (suffix.isEmpty()) target else null)
                }
            }
        }
        staging.mkdirs()
        baselineFile.writeText(JsonObject(baseline).toString() + "\n")
    }
}
