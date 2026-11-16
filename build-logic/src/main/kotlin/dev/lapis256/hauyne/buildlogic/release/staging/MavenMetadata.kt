package dev.lapis256.hauyne.buildlogic.release.staging

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

object MavenMetadata {
    fun verifyPreservedVersions(stagingDirectory: File) {
        val baseline = Json.parseToJsonElement(stagingDirectory.resolve("baseline-metadata.json").readText()).jsonObject
        for ((key, value) in baseline) {
            ReleaseArtifacts.resolve(stagingDirectory, "maven/$key")
            if (!key.endsWith("/maven-metadata.xml")) continue
            val previous = value.jsonObject.getValue("versions").jsonArray.map { it.jsonPrimitive.content }.toSet()
            val staged = stagingDirectory.resolve("maven/$key")
            require(staged.isFile) { "Missing staged metadata: $key" }
            require(versions(staged).toSet().containsAll(previous)) { "Previous Maven versions were lost: $key" }
        }
    }

    internal fun entry(etag: String?, file: File? = null): JsonObject = buildJsonObject {
        put("etag", etag)
        if (file != null) {
            put(
                "versions",
                buildJsonArray {
                    if (etag != null) versions(file).forEach { add(JsonPrimitive(it)) }
                },
            )
        }
    }

    internal fun versions(file: File): List<String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(file)
        require(document.documentElement.tagName == "metadata") { "Invalid Maven metadata: $file" }
        val nodes = XPathFactory.newInstance().newXPath()
            .evaluate("/metadata/versioning/versions/version", document, XPathConstants.NODESET) as NodeList
        return (0 until nodes.length).map { nodes.item(it).textContent.trim() }.also {
            require(it.none(String::isBlank)) { "Empty Maven version: $file" }
        }
    }
}
