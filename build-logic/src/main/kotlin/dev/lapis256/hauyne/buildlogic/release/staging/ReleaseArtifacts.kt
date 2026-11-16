package dev.lapis256.hauyne.buildlogic.release.staging

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

object ReleaseArtifacts {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun resolve(root: File, path: String): File {
        require(path.isNotBlank() && '\\' !in path && ':' !in path && !path.startsWith('/')) { "Invalid release path: $path" }
        require(path.split('/').none { it.isEmpty() || it == "." || it == ".." }) { "Invalid release path: $path" }
        val file = root.resolve(path)
        var current = file.toPath()
        while (current != root.toPath()) {
            require(!Files.isSymbolicLink(current)) { "Symbolic link in release path: $path" }
            current = requireNotNull(current.parent) { "Invalid release path: $path" }
        }
        require(file.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())) { "Invalid release path: $path" }
        return file
    }

    fun files(root: File): Set<String> = root.walkTopDown().onEnter {
        require(!Files.isSymbolicLink(it.toPath())) { "Symbolic link in release directory: $it" }
        true
    }.filter {
        require(!Files.isSymbolicLink(it.toPath())) { "Symbolic link in release directory: $it" }
        it.isFile
    }.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()

    private fun entries(staging: File): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        for (line in staging.resolve("SHA256SUMS").readLines()) {
            require(line.matches(Regex("[0-9a-f]{64}  .+"))) { "Invalid release manifest entry" }
            val (hash, path) = line.split("  ", limit = 2)
            resolve(staging, path)
            require(entries.put(path, hash) == null) { "Duplicate release path: $path" }
        }
        require(entries.keys.any { it.startsWith("maven/") && it.endsWith(".jar") }) { "Release manifest has no JARs" }
        require("baseline-metadata.json" in entries) { "Missing metadata baseline" }
        return entries
    }

    fun readManifest(staging: File): Map<String, String> = entries(staging)
        .filterKeys { it.startsWith("maven/") }.mapKeys { it.key.removePrefix("maven/") }

    fun verifyStaging(staging: File): Map<String, String> {
        val entries = entries(staging)
        require(files(staging) == entries.keys + "SHA256SUMS") { "Release file set changed" }
        for ((path, expected) in entries) {
            require(sha256(resolve(staging, path)) == expected) { "Release artifact changed: $path" }
        }
        return entries.filterKeys { it.startsWith("maven/") }.mapKeys { it.key.removePrefix("maven/") }
    }
}
