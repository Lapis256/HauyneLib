package dev.lapis256.hauyne.buildlogic.release

import java.io.File
import java.security.MessageDigest

object ReleaseArtifacts {
    fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        .joinToString("") { "%02x".format(it) }

    fun verify(root: File, manifest: File, artifacts: Iterable<File> = emptyList()) {
        val entries = manifest.readLines().associate {
            val (hash, path) = it.split("  ", limit = 2)
            path to hash
        }
        require(entries.isNotEmpty()) { "Release manifest is empty" }
        for ((path, expected) in entries) {
            val actual = sha256(root.resolve(path))
            require(actual == expected) { "Release artifact changed: $path" }
        }
        for (artifact in artifacts) {
            require(artifact.relativeTo(root).invariantSeparatorsPath in entries) {
                "Artifact is missing from release manifest: $artifact"
            }
        }
    }
}
