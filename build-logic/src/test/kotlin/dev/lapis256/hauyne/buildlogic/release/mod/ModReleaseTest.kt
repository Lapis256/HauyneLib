package dev.lapis256.hauyne.buildlogic.release.mod

import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress

class ModReleaseTest {
    @TempDir
    lateinit var root: File
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val base get() = "http://127.0.0.1:${server.address.port}"
    private val bytes = "staged final reobfuscated jar".toByteArray()
    private var releases = "[]"
    private var queryStatus = 200
    private var downloadStatus = 200
    private var downloads = 0
    private var queries = 0
    private var downloadBytes = bytes
    private val queryPaths = mutableListOf<String>()

    init {
        server.createContext("/") { exchange ->
            val download = exchange.requestURI.path == "/jar"
            if (download) {
                downloads++
                assertEquals(null, exchange.requestHeaders.getFirst("Authorization"))
                assertEquals(null, exchange.requestHeaders.getFirst("x-api-key"))
            } else {
                queries++
                queryPaths += exchange.requestURI.toString()
            }
            val body = if (download) downloadBytes else releases.toByteArray()
            exchange.sendResponseHeaders(if (download) downloadStatus else queryStatus, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
    }

    @AfterEach
    fun close() = server.stop(0)

    private fun modrinthFile(id: String = "release1", content: ByteArray = bytes) = buildJsonObject {
        put("id", id)
        put("version_number", "1.0-forge")
        put("files", Json.parseToJsonElement("""[{"hashes":{"sha512":"${ModRelease.digest("SHA-512", content)}"},"url":"$base/jar"}]"""))
    }.toString()

    private fun release(platform: String = "modrinth"): ModRelease {
        val jar = root.resolve("staging/maven/dev/test/mod/1.0/mod-1.0.jar")
        if (!jar.exists()) {
            jar.parentFile.mkdirs()
            jar.writeBytes(bytes)
        }
        return ModRelease(
            platform,
            "42",
            "1.0-forge",
            "mod-1.0",
            jar,
            root.resolve("release/results"),
            ModRelease.digest("SHA-256", jar.readBytes()),
            { ModReleaseClient(platform, "42", "fake-token", base) },
        )
    }

    private fun status(): String = root.resolve("release/results").listFiles()!!.single {
        it.name.endsWith(".json") && !it.name.endsWith("-upload.json")
    }.let { Json.parseToJsonElement(it.readText()).jsonObject.getValue("status").jsonPrimitive.content }

    @Test
    fun partialFailureResumesByQueryAndNeverUploadsTheCompletedPlatformAgain() {
        var uploads = 0
        val release = release()
        release.run(false) {
            uploads++
            releases = "[${modrinthFile()}]"
        }
        assertEquals("verified", status())
        release.run(false) {
            uploads++
            error("must not resend")
        }
        assertEquals(1, uploads)
        assertEquals(2, downloads)
        assertTrue(root.resolve("staging").walkTopDown().filter(File::isFile).all { it.extension == "jar" })
    }

    @Test
    fun lostUploadResponseIsReconciledByDownloadingTheRemoteJar() {
        release().run(false) {
            releases = "[${modrinthFile()}]"
            error("response lost")
        }
        assertEquals("verified", status())
    }

    @Test
    fun unknownUploadOutcomeIsNotRetriedUntilRemoteFileAppears() {
        val release = release()
        var uploads = 0
        assertThrows(IllegalStateException::class.java) {
            release.run(false) {
                uploads++
                error("timeout")
            }
        }
        assertEquals("unresolved", status())
        assertThrows(IllegalStateException::class.java) { release.run(false) { uploads++ } }
        assertEquals(1, uploads)
        releases = "[${modrinthFile()}]"
        release.run(false) { uploads++ }
        assertEquals(1, uploads)
        assertEquals("verified", status())
    }

    @Test
    fun unavailableDownloadIsPendingAndRetryOnlyDownloads() {
        val release = release()
        var uploads = 0
        downloadStatus = 404
        assertThrows(IllegalStateException::class.java) {
            release.run(false) {
                uploads++
                releases = "[${modrinthFile()}]"
            }
        }
        assertEquals("pending", status())
        downloadStatus = 200
        release.run(false) { uploads++ }
        assertEquals(1, uploads)
        assertEquals("verified", status())
    }

    @Test
    fun conflictingAmbiguousAndFalselyAdvertisedDigestsNeverUpload() {
        val release = release()
        releases = "[${modrinthFile(content = "different".toByteArray())}]"
        assertThrows(IllegalStateException::class.java) { release.run(false) { error("must not send") } }
        assertEquals("conflict", status())
        releases = "[${modrinthFile()},${modrinthFile("release2")}]"
        assertThrows(IllegalStateException::class.java) { release.run(false) { error("must not send") } }
        releases = "[${modrinthFile()}]"
        downloadBytes = "different".toByteArray()
        assertThrows(IllegalStateException::class.java) { release.run(false) { error("must not send") } }
        assertEquals("conflict", status())
    }

    @Test
    fun transientPreflightFailureDoesNotPreventASafeFirstUpload() {
        val release = release()
        queryStatus = 503
        assertThrows(IllegalStateException::class.java) { release.run(false) { error("must not send") } }
        assertEquals("preflight-failed", status())
        queryStatus = 200
        release.run(false) { releases = "[${modrinthFile()}]" }
        assertEquals("verified", status())
    }

    @Test
    fun dryRunDoesNotReadStagingQuerySecretsOrWriteResults() {
        val release = ModRelease(
            "modrinth",
            "42",
            "1.0",
            "mod",
            root.resolve("missing.jar"),
            root.resolve("release/results"),
            ModRelease.digest("SHA-256", bytes),
            { error("must not require credentials") },
        )
        release.run(true) { error("must not send") }
        assertEquals(0, queries)
        assertFalse(root.resolve("release").exists())
    }

    @Test
    fun curseforgeQueriesProjectFilesAndChecksSha1ThenDownloadedSha256() {
        releases =
            """{"data":[{"id":7,"modId":42,"displayName":"mod-1.0","fileName":"mod-1.0.jar","hashes":[{"algo":1,"value":"${ModRelease.digest(
                "SHA-1",
                bytes,
            )}"}],"downloadUrl":"$base/jar"}],"pagination":{"index":0,"resultCount":1,"totalCount":1}}"""
        release("curseforge").run(false) { error("must not send") }
        assertEquals("verified", status())
        assertEquals(listOf("/mods/42/files?index=0&pageSize=50"), queryPaths)
        assertEquals(1, downloads)
    }
}
