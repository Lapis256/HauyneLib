package dev.lapis256.hauyne.buildlogic.release.r2

import dev.lapis256.hauyne.buildlogic.release.staging.MavenMetadata
import dev.lapis256.hauyne.buildlogic.release.staging.ReleaseArtifacts
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@UntrackedTask(because = "Publishes sealed staging with remote conditional writes")
abstract class UploadStagedMavenToR2 : R2Task() {
    @TaskAction
    fun upload() {
        val staging = stagingDirectory.get().asFile
        val manifest = ReleaseArtifacts.readManifest(staging)
        MavenMetadata.verifyPreservedVersions(staging)
        val baseline = Json.parseToJsonElement(staging.resolve("baseline-metadata.json").readText()).jsonObject
        val metadata = manifest.keys.filter { metadataBody(it) != null }
        manifest.keys.forEach(::requireMavenPath)
        metadata.forEach { key ->
            require(key in baseline && metadataBody(key) in baseline) { "Missing metadata baseline: $key" }
            require(metadataBody(key) in manifest) { "Missing metadata body: $key" }
        }
        val expectedEtags = metadata.associateWith { key ->
            val value = baseline.getValue(key).jsonObject.getValue("etag")
            if (value == JsonNull) null else value.jsonPrimitive.content.also { require(it.isNotBlank()) { "Empty ETag: $key" } }
        }
        // 全成果物、メタデータのチェックサム、本体の順に公開する。
        val ordered = manifest.keys.sortedWith(
            compareBy<String> {
                when {
                    it.endsWith("/maven-metadata.xml") -> 2
                    metadataBody(it) != null -> 1
                    else -> 0
                }
            }.thenBy { it },
        )
        createS3Client().use { client ->
            for (key in ordered) {
                val current = missingOn404 {
                    client.headObject(HeadObjectRequest.builder().bucket(bucket.get()).key(key).build())
                }
                val valid = if (metadataBody(key) != null) {
                    if (current == null) {
                        expectedEtags[key] == null
                    } else {
                        requireNotNull(current.eTag()) { "Missing ETag: $key" } == expectedEtags[key]
                    }
                } else {
                    current == null
                }
                require(valid) { "Remote object conflicts with staging: $key" }
            }
            for (key in ordered) {
                val request = PutObjectRequest.builder().bucket(bucket.get()).key(key)
                val etag = expectedEtags[key]
                if (etag == null) request.ifNoneMatch("*") else request.ifMatch(etag)
                client.putObject(request.build(), RequestBody.fromFile(staging.resolve("maven/$key")))
            }
        }
    }
}
