package dev.lapis256.hauyne.buildlogic.release.r2

import dev.lapis256.hauyne.buildlogic.release.staging.MavenMetadata
import dev.lapis256.hauyne.buildlogic.release.staging.ReleaseArtifacts
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.File

@UntrackedTask(because = "Publishes sealed staging with remote conditional writes")
abstract class UploadStagedMavenToR2 : R2Task() {
    @TaskAction
    fun upload() = R2Results(resultDirectory.get().asFile, "r2-upload").run {
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
            val reusable = mutableSetOf<String>()
            for (key in ordered) {
                val expected = manifest.getValue(key)
                val current = readRemote(client, bucket.get(), key)
                if (current?.sha256 == expected) {
                    reusable += key
                    record(key, "reused", expected)
                    continue
                }
                val valid = if (metadataBody(key) != null) current?.etag == expectedEtags[key] else current == null
                if (!valid) record(key, "conflict", expected)
                require(valid) { "Remote object conflicts with staging: $key" }
                record(key, "pending", expected)
            }
            for (key in ordered) {
                val file = staging.resolve("maven/$key")
                val expected = manifest.getValue(key)
                if (key in reusable) continue
                val isMetadata = metadataBody(key) != null
                record(key, "sending", expected)
                val status = try {
                    put(client, key, file, expected, if (isMetadata) expectedEtags[key] else null)
                } catch (error: Exception) {
                    record(key, if (error is S3Exception && error.statusCode() in listOf(409, 412)) "conflict" else "unconfirmed", expected)
                    throw error
                }
                record(key, status, expected)
            }
        }
    }

    private fun put(client: S3Client, key: String, file: File, expected: String, etag: String?): String {
        val request = PutObjectRequest.builder().bucket(bucket.get()).key(key)
        if (etag == null) request.ifNoneMatch("*") else request.ifMatch(etag)
        try {
            client.putObject(request.build(), RequestBody.fromFile(file))
            return "uploaded"
        } catch (error: SdkException) {
            if (error is S3Exception && error.statusCode() !in listOf(409, 412) && error.statusCode() < 500) throw error
            // 応答喪失や条件競合でも、同じ内容の書き込み完了だけは再利用できる。
            val actual = try {
                readRemote(client, bucket.get(), key)
            } catch (verificationError: Exception) {
                error.addSuppressed(verificationError)
                throw error
            }
            if (actual?.sha256 != expected) throw error
            return "reused"
        }
    }
}
