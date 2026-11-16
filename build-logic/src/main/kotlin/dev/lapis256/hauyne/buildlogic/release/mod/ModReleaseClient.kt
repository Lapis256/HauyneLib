package dev.lapis256.hauyne.buildlogic.release.mod

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

internal data class RemoteModJar(val id: String, val algorithm: String, val digest: String, val url: String?)

internal class ModReleaseClient(private val platform: String, private val project: String, private val token: String, private val endpoint: String) {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()

    fun find(version: String, fileName: String, displayName: String): List<RemoteModJar> = when (platform) {
        "modrinth" -> json("/project/${encode(project)}/version").jsonArray
            .map { it.jsonObject }.filter { it.string("version_number") == version }
            .map { release ->
                val files = release.getValue("files").jsonArray
                require(files.size == 1) { "Ambiguous Modrinth version: ${release.string("id")}" }
                val file = files.single().jsonObject
                RemoteModJar(release.string("id"), "SHA-512", file.getValue("hashes").jsonObject.string("sha512"), file.string("url"))
            }

        "curseforge" -> curseforgeFiles().filter {
            it.string("fileName") == fileName || it.string("displayName") == displayName
        }.map { file ->
            require(file.string("modId") == project) { "CurseForge project mismatch" }
            val hashes = file.getValue("hashes").jsonArray.map { it.jsonObject }.filter { it.getValue("algo").jsonPrimitive.int == 1 }
            require(hashes.size == 1) { "Missing or ambiguous CurseForge SHA-1" }
            RemoteModJar(
                file.string("id"),
                "SHA-1",
                hashes.single().string("value"),
                file["downloadUrl"]?.jsonPrimitive?.content?.takeUnless {
                    it ==
                        "null"
                },
            )
        }

        else -> error("Unsupported Mod platform: $platform")
    }

    fun download(url: String): ByteArray {
        // ダウンロード先には API の認証ヘッダーを渡さない。
        var uri = URI(url)
        repeat(6) {
            require(uri.scheme == "https" || (uri.scheme == "http" && uri.host in listOf("localhost", "127.0.0.1"))) {
                "Insecure Mod download URL"
            }
            val response = http.send(request(uri).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
            if (response.statusCode() in listOf(301, 302, 303, 307, 308)) {
                uri = uri.resolve(response.headers().firstValue("Location").orElseThrow())
            } else {
                check(response.statusCode() == 200) { "Mod download unavailable: HTTP ${response.statusCode()}" }
                return response.body()
            }
        }
        error("Too many Mod download redirects")
    }

    private fun curseforgeFiles(): List<JsonObject> {
        val files = mutableListOf<JsonObject>()
        var index = 0
        while (index < 10_000) {
            val response = json("/mods/${encode(project)}/files?index=$index&pageSize=50").jsonObject
            val page = response.getValue("data").jsonArray
            val pagination = response.getValue("pagination").jsonObject
            require(pagination.getValue("index").jsonPrimitive.int == index) { "Invalid CurseForge pagination" }
            require(pagination.getValue("resultCount").jsonPrimitive.int == page.size) { "Incomplete CurseForge page" }
            files += page.map { it.jsonObject }
            index += page.size
            if (index >= pagination.getValue("totalCount").jsonPrimitive.int) return files
            check(page.isNotEmpty()) { "Incomplete CurseForge file listing" }
        }
        error("Cannot exhaust CurseForge file listing; refusing an ambiguous upload")
    }

    private fun json(path: String) = http.send(
        request(URI(endpoint.trimEnd('/') + path))
            .header(if (platform == "modrinth") "Authorization" else "x-api-key", token).GET().build(),
        HttpResponse.BodyHandlers.ofString(),
    ).let {
        check(it.statusCode() == 200) { "$platform query failed: HTTP ${it.statusCode()}" }
        Json.parseToJsonElement(it.body())
    }

    private fun request(uri: URI) = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(120))
        .header("User-Agent", "HauyneLib-release/1 (https://github.com/Lapis256/HauyneLib)")

    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8)
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
}
