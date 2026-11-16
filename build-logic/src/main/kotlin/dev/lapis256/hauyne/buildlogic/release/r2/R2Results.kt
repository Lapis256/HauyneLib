package dev.lapis256.hauyne.buildlogic.release.r2

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

internal class R2Results(private val directory: File, private val name: String) {
    private val entries = mutableMapOf<String, JsonElement>()
    private val result = mutableMapOf<String, JsonElement>("status" to JsonPrimitive("running"))

    fun run(action: R2Results.() -> Unit) {
        save()
        try {
            action()
            result["status"] = JsonPrimitive("success")
        } catch (error: Exception) {
            result["status"] = JsonPrimitive("failed")
            result["errorType"] = JsonPrimitive(error.javaClass.simpleName)
            throw error
        } finally {
            save()
        }
    }

    fun record(key: String, status: String, sha256: String? = null) {
        entries[key] = buildJsonObject {
            put("status", status)
            if (sha256 != null) put("sha256", sha256)
        }
        save()
    }

    private fun save() {
        directory.mkdirs()
        directory.resolve("$name.json").writeText(JsonObject(result + ("objects" to JsonObject(entries))).toString() + "\n")
    }
}
