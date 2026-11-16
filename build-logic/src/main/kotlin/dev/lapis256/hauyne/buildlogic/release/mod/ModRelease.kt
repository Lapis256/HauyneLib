package dev.lapis256.hauyne.buildlogic.release.mod

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.security.MessageDigest

internal class ModRelease(
    private val platform: String,
    private val project: String,
    private val version: String,
    private val displayName: String,
    private val jar: File,
    private val results: File,
    private val expected: String,
    private val client: () -> ModReleaseClient,
) {
    private val key = digest("SHA-256", "$platform:$project:$version:${jar.name}".toByteArray())
    private val record = results.resolve("mod-$key.json")
    val uploadResult: File = results.resolve("mod-$key-upload.json")

    fun run(dryRun: Boolean, upload: (() -> Unit)? = null) {
        if (dryRun) return
        results.mkdirs()
        FileChannel.open(results.resolve("mod-$key.lock").toPath(), CREATE, WRITE).use { channel ->
            val lock = checkNotNull(channel.tryLock()) { "Another Mod publish is in progress" }
            lock.use { execute(upload) }
        }
    }

    private fun execute(upload: (() -> Unit)?) {
        val previous = if (record.exists()) Json.parseToJsonElement(record.readText()).jsonObject else null
        require(previous == null || previous.getValue("sha256").jsonPrimitive.content == expected) { "Mod release input changed" }
        var attempted = previous != null && previous.string("status") != "preflight-failed"
        try {
            val api = client()
            if (verifyRemote(api, expected)) return
            check(!attempted && !uploadResult.exists()) { "Previous Mod upload is unresolved; refusing to resend" }
            check(upload != null) { "Mod release has not been published" }
            save("uploading", expected)
            attempted = true
            try {
                upload()
            } catch (failure: Exception) {
                // 応答喪失時も照会だけ行い、アップロードは繰り返さない。
                if (runCatching { verifyRemote(api, expected) }.getOrDefault(false)) return
                throw failure
            }
            save("pending", expected)
            check(verifyRemote(api, expected)) { "Uploaded Mod JAR is not yet visible; verification must be retried" }
        } catch (failure: Exception) {
            val status = if (record.exists()) Json.parseToJsonElement(record.readText()).jsonObject.string("status") else null
            if (status !in listOf("conflict", "pending")) save(if (attempted) "unresolved" else "preflight-failed", expected)
            throw failure
        }
    }

    private fun verifyRemote(api: ModReleaseClient, expected: String): Boolean {
        val matches = api.find(version, jar.name, displayName)
        if (matches.isEmpty()) return false
        if (matches.size != 1) {
            save("conflict", expected)
            error("Ambiguous $platform release: multiple matching files or versions")
        }
        val remote = matches.single()
        save("pending", expected, remote.id)
        val url = checkNotNull(remote.url) { "Mod download URL is not yet available" }
        val downloaded = api.download(url)
        check(digest("SHA-256", downloaded) == expected && remote.digest.equals(digest(remote.algorithm, downloaded), ignoreCase = true)) {
            save("conflict", expected, remote.id)
            "Downloaded $platform JAR differs from staging"
        }
        save("verified", expected, remote.id)
        return true
    }

    private fun save(status: String, expected: String, remoteId: String? = null) {
        val responseId = if (uploadResult.isFile) {
            val response = Json.parseToJsonElement(uploadResult.readText()).jsonObject
            (response["fileId"] ?: response["id"])?.jsonPrimitive?.content
        } else {
            null
        }
        val data = buildJsonObject {
            put("platform", platform)
            put("project", project)
            put("version", version)
            put("file", jar.name)
            put("sha256", expected)
            put("status", status)
            (remoteId ?: responseId)?.let { put("remoteId", it) }
        }
        val temporary = record.resolveSibling("${record.name}.tmp")
        temporary.writeText(data.toString() + "\n")
        Files.move(temporary.toPath(), record.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
    }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    companion object {
        fun digest(algorithm: String, bytes: ByteArray): String = MessageDigest.getInstance(algorithm).digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
