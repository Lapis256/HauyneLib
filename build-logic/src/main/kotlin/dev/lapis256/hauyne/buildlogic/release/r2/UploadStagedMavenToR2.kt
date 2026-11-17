package dev.lapis256.hauyne.buildlogic.release.r2

import kotlinx.serialization.json.Json
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File

@UntrackedTask(because = "Publishes staged files with remote conditional writes")
abstract class UploadStagedMavenToR2 : R2Task() {
    @TaskAction
    fun upload() {
        val staging = stagingDirectory.get().asFile
        val maven = staging.resolve("maven")
        require(maven.isDirectory) { "Missing staged Maven directory: $maven" }
        val keys = maven.walkTopDown().filter { it.isFile }.map { it.relativeTo(maven).invariantSeparatorsPath }.toList()
        require(keys.isNotEmpty()) { "No staged Maven files" }
        val etags = readEtags(staging, keys)
        val ordered = keys.sortedWith(
            compareBy<String> {
                when {
                    it.endsWith("/maven-metadata.xml") -> 2
                    metadataBody(it) != null -> 1
                    else -> 0
                }
            }.thenBy { it },
        )
        createS3Client().use { client ->
            for (key in ordered) uploadFile(client, maven, key, etags[key])
        }
    }

    private fun readEtags(staging: File, keys: List<String>): Map<String, String?> {
        val etags = Json.decodeFromString<Map<String, String?>>(staging.resolve("metadata-etags.json").readText())
        return keys.filter { metadataBody(it) != null }.associateWith(etags::getValue)
    }

    private fun uploadFile(client: S3Client, maven: File, key: String, etag: String?) {
        val request = PutObjectRequest.builder().bucket(bucket.get()).key(key)
        if (etag == null) request.ifNoneMatch("*") else request.ifMatch(etag)
        client.putObject(request.build(), RequestBody.fromFile(maven.resolve(key)))
    }
}
