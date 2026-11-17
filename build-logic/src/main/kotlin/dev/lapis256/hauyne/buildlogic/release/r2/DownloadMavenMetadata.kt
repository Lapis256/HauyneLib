package dev.lapis256.hauyne.buildlogic.release.r2

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import java.io.File

@UntrackedTask(because = "Reads the current remote metadata and ETags")
abstract class DownloadMavenMetadata : R2Task() {
    @get:Internal
    abstract val coordinates: ListProperty<String>

    @TaskAction
    fun download() {
        val staging = stagingDirectory.get().asFile
        require(!staging.exists()) { "Staging already exists: $staging. Clean staging before preparing a release." }
        val modules = coordinates.get().distinct().sorted()
        require(modules.isNotEmpty()) { "No Maven coordinates registered" }
        modules.forEach {
            requireMavenPath(it)
            require('/' in it) { "Expected group-path/artifact: $it" }
        }
        check(staging.mkdirs()) { "Cannot create staging: $staging" }
        val etags = mutableMapOf<String, String?>()
        createS3Client().use { client ->
            for (module in modules) {
                etags.putAll(downloadModuleMetadata(client, staging, module))
            }
        }
        staging.resolve("metadata-etags.json").writeText(Json.encodeToString(etags) + "\n")
    }

    private fun downloadModuleMetadata(client: S3Client, staging: File, module: String): Map<String, String?> {
        val key = "$module/maven-metadata.xml"
        val etags = mutableMapOf<String, String?>()
        etags[key] = missingOn404 {
            client.getObject(GetObjectRequest.builder().bucket(bucket.get()).key(key).build()).use { input ->
                val etag = requireNotNull(input.response().eTag()) { "Missing ETag: $key" }
                require(etag.isNotBlank()) { "Empty ETag: $key" }
                val target = staging.resolve("maven/$key")
                check(target.parentFile.mkdirs()) { "Cannot create metadata directory: ${target.parentFile}" }
                target.outputStream().use { input.copyTo(it) }
                etag
            }
        }
        for (suffix in metadataSuffixes.filter { it.isNotEmpty() }) {
            val checksumKey = key + suffix
            etags[checksumKey] = missingOn404 {
                val response = client.headObject(HeadObjectRequest.builder().bucket(bucket.get()).key(checksumKey).build())
                requireNotNull(response.eTag()) { "Missing ETag: $checksumKey" }.also {
                    require(it.isNotBlank()) { "Empty ETag: $checksumKey" }
                }
            }
        }
        return etags
    }
}
