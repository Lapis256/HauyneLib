package dev.lapis256.hauyne.buildlogic.release.r2

import kotlinx.serialization.json.Json
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
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
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
            }
        }

    private fun stage(files: Map<String, String>, etags: Map<String, String?> = emptyMap()) {
        (mapOf(jar to "jar") + files).forEach { (key, body) ->
            staging.resolve("maven/$key").apply {
                parentFile.mkdirs()
                writeText(body)
            }
        }
        staging.mkdirs()
        val json = buildJsonObject {
            etags.forEach { (key, etag) -> put(key, etag) }
        }
        staging.resolve("metadata-etags.json").writeText(json.toString())
    }

    @Test
    fun downloadsMetadataAndRecordsOpaqueEtagsAndAbsence() {
        remote.objects[metadata] = Stored("<metadata/>".toByteArray(), "\"opaque-not-a-sha256\"")
        remote.objects["$metadata.sha1"] = Stored("old-checksum".toByteArray(), "\"checksum-etag\"")
        task(Download::class.java).download()
        assertEquals("<metadata/>", staging.resolve("maven/$metadata").readText())
        val etags = Json.parseToJsonElement(staging.resolve("metadata-etags.json").readText()).jsonObject
        assertEquals("\"opaque-not-a-sha256\"", etags.getValue(metadata).jsonPrimitive.content)
        assertEquals("\"checksum-etag\"", etags.getValue("$metadata.sha1").jsonPrimitive.content)
        assertTrue(etags.getValue("$metadata.sha256") == JsonNull)
        assertEquals(5, etags.size)
        assertEquals(listOf(metadata), remote.gets)
        assertEquals(metadataSuffixes.drop(1).map { metadata + it }, remote.heads)
        assertFalse(staging.resolve("maven/$metadata.sha1").exists())
    }

    @Test
    fun only404IsAcceptedForMetadataBodiesAndChecksumEtags() {
        for (status in listOf(403, 429, 500)) {
            for (checksum in listOf(false, true)) {
                val directory = root.resolve("failure-$status-$checksum")
                remote.getFailure = if (checksum) null else s3Error(status)
                remote.headFailure = if (checksum) s3Error(status) else null
                val download = task(Download::class.java).apply { stagingDirectory.set(directory) }
                assertEquals(status, assertThrows(S3Exception::class.java) { download.download() }.statusCode())
                assertFalse(directory.resolve("metadata-etags.json").exists())
            }
        }
    }

    @Test
    fun downloadRequiresNonemptyEtagsForExistingMetadataAndChecksums() {
        for (etag in listOf(null, "")) {
            for (key in listOf(metadata, "$metadata.sha1")) {
                val directory = root.resolve("etag-${etag == null}-${key.endsWith(".sha1")}")
                remote.objects.clear()
                remote.objects[key] = Stored("metadata".toByteArray(), etag)
                val download = task(Download::class.java).apply { stagingDirectory.set(directory) }
                assertThrows(IllegalArgumentException::class.java) { download.download() }
                assertFalse(directory.resolve("metadata-etags.json").exists())
            }
        }
    }

    @Test
    fun absentMetadataRecordsNullWithoutCreatingMetadataBody() {
        task(Download::class.java).download()
        assertFalse(staging.resolve("maven/$metadata").exists())
        val etags = Json.parseToJsonElement(staging.resolve("metadata-etags.json").readText()).jsonObject
        assertEquals(5, etags.size)
        assertTrue(etags.values.all { it == JsonNull })
    }

    @Test
    fun downloadRejectsUnsafeCoordinatesAndExistingStagingBeforeNetwork() {
        val download = task(Download::class.java)
        for (coordinate in listOf(
            "../artifact",
            "/absolute/artifact",
            "dev\\artifact",
            "dev//artifact",
            "dev/./artifact",
            "C:/artifact",
            "artifact",
        )) {
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
        val otherChecksum = "$otherMetadata.sha1"
        val pom = jar.removeSuffix(".jar") + ".pom"
        remote.objects[metadata] = Stored("old".toByteArray(), "\"old-body\"")
        remote.objects[checksum] = Stored("old-checksum".toByteArray(), "\"old-checksum\"")
        stage(
            mapOf(
                otherMetadata to "<metadata>other</metadata>",
                otherChecksum to "other-checksum",
                pom to "pom",
                metadata to "<metadata>new</metadata>",
                checksum to "new-checksum",
            ),
            mapOf(metadata to "\"old-body\"", checksum to "\"old-checksum\"", otherMetadata to null, otherChecksum to null),
        )
        task(Upload::class.java).upload()
        assertEquals(listOf(jar, pom, checksum, otherChecksum, metadata, otherMetadata), remote.puts.map { it.key() })
        assertEquals("*", remote.puts[0].ifNoneMatch())
        assertEquals("*", remote.puts[1].ifNoneMatch())
        assertEquals("\"old-checksum\"", remote.puts[2].ifMatch())
        assertEquals("*", remote.puts[3].ifNoneMatch())
        assertEquals("\"old-body\"", remote.puts[4].ifMatch())
        assertEquals("*", remote.puts[5].ifNoneMatch())
        assertFalse(remote.objects.containsKey("metadata-etags.json"))
        assertTrue(remote.heads.isEmpty())
        assertTrue(remote.gets.isEmpty())
    }

    @Test
    fun refusesExistingImmutableObjectEvenWithSameContentAndDoesNotPublishMetadata() {
        remote.objects[jar] = Stored("jar".toByteArray(), "\"existing\"")
        stage(mapOf(jar to "jar", metadata to "<metadata>new</metadata>"), mapOf(metadata to null))
        assertEquals(412, assertThrows(S3Exception::class.java) { task(Upload::class.java).upload() }.statusCode())
        assertEquals(listOf(jar), remote.puts.map { it.key() })
        assertEquals("jar", remote.objects.getValue(jar).body.toString(Charsets.UTF_8))
        assertTrue(remote.heads.isEmpty())
        assertTrue(remote.gets.isEmpty())
    }

    @Test
    fun putFailuresPropagateWithoutPublishingMetadata() {
        stage(mapOf(metadata to "<metadata/>"), mapOf(metadata to null))
        for (status in listOf(403, 404, 429, 500)) {
            remote.putFailure = s3Error(status)
            assertEquals(status, assertThrows(S3Exception::class.java) { task(Upload::class.java).upload() }.statusCode())
            assertFalse(remote.objects.containsKey(jar))
            assertFalse(remote.objects.containsKey(metadata))
        }
        assertTrue(remote.puts.all { it.key() == jar })
        assertTrue(remote.heads.isEmpty())
        assertTrue(remote.gets.isEmpty())
    }

    @Test
    fun checksumConflictStopsBeforeMetadataBodyPut() {
        val checksum = "$metadata.sha256"
        remote.objects[checksum] = Stored("someone-else".toByteArray(), "\"concurrent\"")
        stage(
            mapOf(checksum to "new-checksum", metadata to "<metadata/>"),
            mapOf(metadata to null, checksum to "\"old\""),
        )
        assertEquals(412, assertThrows(S3Exception::class.java) { task(Upload::class.java).upload() }.statusCode())
        assertEquals(listOf(jar, checksum), remote.puts.map { it.key() })
        assertFalse(remote.objects.containsKey(metadata))
    }

    @Test
    fun missingEtagEntryFailsBeforeUpload() {
        stage(mapOf(jar to "jar", metadata to "<metadata>new</metadata>"))
        assertThrows(IllegalArgumentException::class.java) { task(Upload::class.java).upload() }
        assertTrue(remote.puts.isEmpty())
        assertTrue(remote.heads.isEmpty())
        assertTrue(remote.gets.isEmpty())
    }

    @Test
    fun invalidEtagValuesFailBeforeAnyPut() {
        stage(mapOf(metadata to "<metadata/>"), mapOf(metadata to null))
        val etagsFile = staging.resolve("metadata-etags.json")
        for (value in listOf("42", "true", "\"\"", "{}")) {
            etagsFile.writeText("{\"$metadata\":$value}")
            assertThrows(IllegalArgumentException::class.java) { task(Upload::class.java).upload() }
            assertTrue(remote.puts.isEmpty())
        }
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

    private data class Stored(val body: ByteArray, val etag: String?)

    private class FakeS3 {
        val objects = mutableMapOf<String, Stored>()
        val gets = mutableListOf<String>()
        val heads = mutableListOf<String>()
        val puts = mutableListOf<PutObjectRequest>()
        var getFailure: Exception? = null
        var headFailure: Exception? = null
        var putFailure: Exception? = null
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

                    "headObject" -> {
                        val request = args[0] as HeadObjectRequest
                        assertEquals("releases", request.bucket())
                        heads += request.key()
                        headFailure?.let { throw it }
                        val stored = objects[request.key()] ?: throw s3Error(404)
                        HeadObjectResponse.builder().eTag(stored.etag).build()
                    }

                    "putObject" -> {
                        val request = args[0] as PutObjectRequest
                        assertEquals("releases", request.bucket())
                        puts += request
                        putFailure?.let { throw it }
                        val existing = objects[request.key()]
                        if ((request.ifNoneMatch() == "*" && existing != null) ||
                            (request.ifMatch() != null && request.ifMatch() != existing?.etag)
                        ) {
                            throw s3Error(412)
                        }
                        val body = (args[1] as RequestBody).contentStreamProvider().newStream().use { it.readBytes() }
                        objects[request.key()] = Stored(body, "\"updated-${puts.size}\"")
                        PutObjectResponse.builder().eTag(objects.getValue(request.key()).etag).build()
                    }

                    "close" -> null

                    else -> error("Unexpected S3 call: ${method.name}")
                }
            },
        )
    }

    companion object {
        private fun s3Error(status: Int) = S3Exception.builder().apply {
            statusCode(status)
            message("S3 $status")
        }.build()
    }
}
