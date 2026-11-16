package dev.lapis256.hauyne.buildlogic.release.r2

import dev.lapis256.hauyne.buildlogic.release.staging.MavenMetadata
import dev.lapis256.hauyne.buildlogic.release.staging.ReleaseArtifacts
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.gradle.api.tasks.Internal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.File
import java.lang.reflect.Proxy

class R2TasksTest {
    @TempDir
    lateinit var root: File

    private val module = "dev/lapis256/example"
    private val metadata = "$module/maven-metadata.xml"
    private val jar = "$module/1.0/example-1.0.jar"
    private val remote = FakeS3()
    private val staging get() = root.resolve("staging")

    private fun <T : R2Task> task(type: Class<T>): T = ProjectBuilder.builder().withProjectDir(root).build()
        .tasks.create("r2", type).apply {
            stagingDirectory.set(staging)
            bucket.set("releases")
            when (this) {
                is Download -> {
                    client = remote.client
                    coordinates.set(listOf(module))
                }

                is Upload -> client = remote.client

                is Verify -> client = remote.client
            }
        }

    private fun stage(files: Map<String, String>, baseline: Map<String, String?> = emptyMap()) {
        (mapOf(jar to "jar") + files).forEach { (key, body) ->
            staging.resolve("maven/$key").apply {
                parentFile.mkdirs()
                writeText(body)
            }
        }
        staging.mkdirs()
        val json = buildJsonObject {
            baseline.forEach { (key, etag) ->
                put(
                    key,
                    buildJsonObject {
                        put("etag", etag)
                        if (key.endsWith("/maven-metadata.xml")) put("versions", JsonArray(emptyList()))
                    },
                )
            }
        }
        staging.resolve("baseline-metadata.json").writeText(json.toString())
        val entries = staging.walkTopDown().filter { it.isFile && it.name != "SHA256SUMS" }.toList()
        staging.resolve("SHA256SUMS").writeText(
            entries.joinToString("") {
                "${ReleaseArtifacts.sha256(it)}  ${it.relativeTo(staging).invariantSeparatorsPath}\n"
            },
        )
    }

    @Test
    fun downloadsMetadataAndRecordsOpaqueEtagsAndAbsence() {
        remote.objects[metadata] = Stored("<metadata/>".toByteArray(), "\"opaque-not-a-sha256\"")
        remote.objects["$metadata.sha1"] = Stored("old-checksum".toByteArray(), "\"checksum-etag\"")
        task(Download::class.java).download()
        assertEquals("<metadata/>", staging.resolve("maven/$metadata").readText())
        val baseline = Json.parseToJsonElement(staging.resolve("baseline-metadata.json").readText()).jsonObject
        assertEquals("\"opaque-not-a-sha256\"", baseline.getValue(metadata).jsonObject.getValue("etag").jsonPrimitive.content)
        assertEquals("\"checksum-etag\"", baseline.getValue("$metadata.sha1").jsonObject.getValue("etag").jsonPrimitive.content)
        assertTrue(baseline.getValue("$metadata.sha256").jsonObject.getValue("etag") == JsonNull)
        assertEquals(5, baseline.size)
    }

    @Test
    fun only404IsAcceptedAndFailedDownloadInvalidatesOldBaseline() {
        for (status in listOf(403, 429, 500)) {
            staging.mkdirs()
            staging.resolve("baseline-metadata.json").writeText("{}")
            remote.getFailure = s3Error(status)
            assertThrows(S3Exception::class.java) { task(Download::class.java).download() }
            assertFalse(staging.resolve("baseline-metadata.json").exists())
        }
    }

    @Test
    fun absentRemoteRemovesStaleLocalMetadata() {
        staging.resolve("maven/$metadata").apply {
            parentFile.mkdirs()
            writeText("stale")
        }
        task(Download::class.java).download()
        assertFalse(staging.resolve("maven/$metadata").exists())
        assertTrue(
            Json.parseToJsonElement(staging.resolve("baseline-metadata.json").readText())
                .jsonObject.getValue(metadata).jsonObject.getValue("etag") == JsonNull,
        )
    }

    @Test
    fun downloadRejectsUnsafeCoordinatesAndSealedStagingBeforeNetwork() {
        val download = task(Download::class.java)
        for (coordinate in listOf("../artifact", "/absolute/artifact", "dev\\artifact", "dev//artifact", "artifact")) {
            download.coordinates.set(listOf(coordinate))
            assertThrows(IllegalArgumentException::class.java) { download.download() }
        }
        download.coordinates.set(listOf(module))
        stage(mapOf(jar to "jar"))
        assertThrows(IllegalArgumentException::class.java) { download.download() }
        assertTrue(remote.gets.isEmpty())
    }

    @Test
    fun uploadsImmutableFilesThenAllMetadataChecksumsThenAllBodiesWithCas() {
        val otherMetadata = "dev/lapis256/other/maven-metadata.xml"
        val checksum = "$metadata.sha256"
        remote.objects[metadata] = Stored("old".toByteArray(), "\"old-body\"")
        remote.objects[checksum] = Stored("old-checksum".toByteArray(), "\"old-checksum\"")
        stage(
            mapOf(jar to "jar", metadata to xml("new"), checksum to "new-checksum", otherMetadata to xml("other")),
            mapOf(metadata to "\"old-body\"", checksum to "\"old-checksum\"", otherMetadata to null),
        )
        task(Upload::class.java).upload()
        assertEquals(listOf(jar, checksum, metadata, otherMetadata), remote.puts.map { it.key() })
        assertEquals("*", remote.puts[0].ifNoneMatch())
        assertEquals("\"old-checksum\"", remote.puts[1].ifMatch())
        assertEquals("\"old-body\"", remote.puts[2].ifMatch())
        assertEquals("*", remote.puts[3].ifNoneMatch())
        assertFalse(remote.objects.containsKey("baseline-metadata.json"))
    }

    @Test
    fun retryReusesSameContentIncludingMetadataWhoseEtagAlreadyChanged() {
        remote.objects[jar] = Stored("jar".toByteArray(), "\"not-the-hash\"")
        remote.objects[metadata] = Stored(xml("new").toByteArray(), "\"new-etag\"")
        stage(mapOf(jar to "jar", metadata to xml("new")), mapOf(metadata to "\"old-etag\""))
        task(Upload::class.java).upload()
        assertTrue(remote.puts.isEmpty())
    }

    @Test
    fun refusesDifferentImmutableObjectAndDoesNotPublishMetadata() {
        remote.objects[jar] = Stored("different".toByteArray(), "\"existing\"")
        stage(mapOf(jar to "jar", metadata to xml("new")), mapOf(metadata to null))
        assertThrows(IllegalArgumentException::class.java) { task(Upload::class.java).upload() }
        assertTrue(remote.puts.isEmpty())
    }

    @Test
    fun failedPreflightNeverWritesAndDoesNotTreatForbiddenAsAbsence() {
        stage(mapOf(jar to "jar"))
        remote.getFailure = s3Error(403)
        assertThrows(S3Exception::class.java) { task(Upload::class.java).upload() }
        assertTrue(remote.puts.isEmpty())
    }

    @Test
    fun metadataConflictStopsBeforeBodyPut() {
        remote.objects[metadata] = Stored("someone-else".toByteArray(), "\"concurrent\"")
        stage(
            mapOf(jar to "jar", "$metadata.sha256" to "new-checksum", metadata to xml("new")),
            mapOf(metadata to "\"old\"", "$metadata.sha256" to null),
        )
        assertThrows(IllegalArgumentException::class.java) { task(Upload::class.java).upload() }
        assertTrue(remote.puts.isEmpty())
    }

    @Test
    fun conditionalPutRefusesConcurrentImmutableCreation() {
        stage(mapOf(jar to "jar"))
        remote.beforePut = { remote.objects[jar] = Stored("racer".toByteArray(), "\"racer\"") }
        assertThrows(S3Exception::class.java) { task(Upload::class.java).upload() }
        assertEquals("racer", remote.objects.getValue(jar).body.toString(Charsets.UTF_8))
    }

    @Test
    fun conditionalMetadataPutRefusesConcurrentUpdate() {
        remote.objects[metadata] = Stored("old".toByteArray(), "\"old\"")
        stage(mapOf(metadata to xml("new")), mapOf(metadata to "\"old\""))
        remote.beforePut = { remote.objects[metadata] = Stored("racer".toByteArray(), "\"racer\"") }
        assertThrows(S3Exception::class.java) { task(Upload::class.java).upload() }
        assertEquals("racer", remote.objects.getValue(metadata).body.toString(Charsets.UTF_8))
    }

    @Test
    fun reusesConcurrentSameContentAndLostPutResponse() {
        val upload = task(Upload::class.java)
        stage(mapOf(jar to "jar"))
        remote.beforePut = { remote.objects[jar] = Stored("jar".toByteArray(), "\"racer\"") }
        upload.upload()
        remote.beforePut = null
        remote.objects.clear()
        remote.loseResponse = true
        upload.upload()
        assertEquals("jar", remote.objects.getValue(jar).body.toString(Charsets.UTF_8))
    }

    @Test
    fun missingBaselineEntryFailsBeforeUpload() {
        stage(mapOf(jar to "jar", metadata to xml("new")))
        assertThrows(IllegalArgumentException::class.java) { task(Upload::class.java).upload() }
        assertTrue(remote.gets.isEmpty())
    }

    @Test
    fun baselineSnapshotsVersionsAndRejectsVersionLoss() {
        remote.objects[metadata] = Stored(xml("1.0", "1.1").toByteArray(), "\"old\"")
        task(Download::class.java).download()
        val staged = staging.resolve("maven/$metadata")
        assertEquals(listOf("1.0", "1.1"), MavenMetadata.versions(staged))
        staged.writeText(xml("1.0", "1.1", "1.2"))
        MavenMetadata.verifyPreservedVersions(staging)
        staged.writeText(xml("1.2"))
        assertThrows(IllegalArgumentException::class.java) { MavenMetadata.verifyPreservedVersions(staging) }
    }

    @Test
    fun metadataParserRejectsExternalEntities() {
        val file = root.resolve("metadata.xml").apply {
            writeText("""<!DOCTYPE metadata [<!ENTITY external SYSTEM "file:///not-to-be-read">]><metadata>&external;</metadata>""")
        }
        assertThrows(org.xml.sax.SAXParseException::class.java) { MavenMetadata.versions(file) }
    }

    @Test
    fun persistsUploadAndVerificationResultsOnSuccessAndFailure() {
        stage(mapOf(jar to "jar"))
        val upload = task(Upload::class.java)
        upload.upload()
        val results = root.resolve("release/results")
        val uploaded = Json.parseToJsonElement(results.resolve("r2-upload.json").readText()).jsonObject
        assertEquals("success", uploaded.getValue("status").jsonPrimitive.content)
        assertEquals("uploaded", uploaded.getValue("objects").jsonObject.getValue(jar).jsonObject.getValue("status").jsonPrimitive.content)
        remote.objects[jar] = Stored("changed".toByteArray(), "\"other\"")
        assertThrows(IllegalArgumentException::class.java) { upload.upload() }
        val failed = Json.parseToJsonElement(results.resolve("r2-upload.json").readText()).jsonObject
        assertEquals("failed", failed.getValue("status").jsonPrimitive.content)
        assertEquals("conflict", failed.getValue("objects").jsonObject.getValue(jar).jsonObject.getValue("status").jsonPrimitive.content)
        val verify = task(Verify::class.java)
        assertThrows(IllegalArgumentException::class.java) { verify.verify() }
        val verification = Json.parseToJsonElement(results.resolve("r2-verify.json").readText()).jsonObject
        assertEquals("failed", verification.getValue("status").jsonPrimitive.content)
        assertEquals("mismatch", verification.getValue("objects").jsonObject.getValue(jar).jsonObject.getValue("status").jsonPrimitive.content)
    }

    @Test
    fun verifiesAllPublishedJarsAndRejectsMissingChangedAndForbiddenObjects() {
        val sources = jar.removeSuffix(".jar") + "-sources.jar"
        stage(mapOf(jar to "jar", sources to "sources", "$module/1.0/example.pom" to "pom"))
        remote.objects[jar] = Stored("jar".toByteArray(), "\"jar\"")
        remote.objects[sources] = Stored("sources".toByteArray(), "\"sources\"")
        val verify = task(Verify::class.java)
        verify.verify()
        assertEquals(setOf(jar, sources), remote.gets.toSet())
        remote.objects.remove(sources)
        assertThrows(IllegalArgumentException::class.java) { verify.verify() }
        remote.objects[sources] = Stored("changed".toByteArray(), "\"sources\"")
        assertThrows(IllegalArgumentException::class.java) { verify.verify() }
        remote.getFailure = s3Error(403)
        assertThrows(S3Exception::class.java) { verify.verify() }
    }

    abstract class Download : DownloadMavenMetadata() {
        @get:Internal
        lateinit var client: S3Client
        override fun createS3Client(): S3Client = client
    }

    abstract class Upload : UploadStagedMavenToR2() {
        @get:Internal
        lateinit var client: S3Client
        override fun createS3Client(): S3Client = client
    }

    abstract class Verify : VerifyPublishedMaven() {
        @get:Internal
        lateinit var client: S3Client
        override fun createS3Client(): S3Client = client
    }

    private data class Stored(val body: ByteArray, val etag: String)

    private class FakeS3 {
        val objects = mutableMapOf<String, Stored>()
        val gets = mutableListOf<String>()
        val puts = mutableListOf<PutObjectRequest>()
        var getFailure: Exception? = null
        var beforePut: (() -> Unit)? = null
        var loseResponse = false
        val client = S3Client::class.java.cast(
            Proxy.newProxyInstance(
                S3Client::class.java.classLoader,
                arrayOf(S3Client::class.java),
            ) { _, method, args ->
                when (method.name) {
                    "getObject" -> {
                        val request = args[0] as GetObjectRequest
                        assertEquals("releases", request.bucket())
                        gets += request.key()
                        getFailure?.let { throw it }
                        val stored = objects[request.key()] ?: throw s3Error(404)
                        ResponseInputStream(
                            GetObjectResponse.builder().eTag(stored.etag).build(),
                            AbortableInputStream.create(stored.body.inputStream()),
                        )
                    }

                    "putObject" -> {
                        val request = args[0] as PutObjectRequest
                        assertEquals("releases", request.bucket())
                        puts += request
                        beforePut?.invoke()
                        val existing = objects[request.key()]
                        if ((request.ifNoneMatch() == "*" && existing != null) ||
                            (request.ifMatch() != null && request.ifMatch() != existing?.etag)
                        ) {
                            throw s3Error(412)
                        }
                        val body = (args[1] as RequestBody).contentStreamProvider().newStream().use { it.readBytes() }
                        objects[request.key()] = Stored(body, "\"updated-${puts.size}\"")
                        if (loseResponse) throw SdkClientException.create("Response lost")
                        PutObjectResponse.builder().eTag(objects.getValue(request.key()).etag).build()
                    }

                    "close" -> null

                    else -> error("Unexpected S3 call: ${method.name}")
                }
            },
        )
    }

    companion object {
        private fun xml(vararg versions: String): String = "<metadata><versioning><versions>" +
            versions.joinToString("") { "<version>$it</version>" } + "</versions></versioning></metadata>"

        private fun s3Error(status: Int) = S3Exception.builder().apply {
            statusCode(status)
            message("S3 $status")
        }.build()
    }
}
