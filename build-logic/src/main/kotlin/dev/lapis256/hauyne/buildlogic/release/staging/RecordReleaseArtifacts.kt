package dev.lapis256.hauyne.buildlogic.release.staging

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class RecordReleaseArtifacts : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Internal
    abstract val stagingDirectory: DirectoryProperty

    @get:Input
    abstract val publicationFiles: MapProperty<String, String>

    @TaskAction
    fun record() {
        val root = rootDirectory.get().asFile
        val staging = stagingDirectory.get().asFile
        require(!staging.resolve("SHA256SUMS").exists()) { "Release staging is already sealed" }
        val publications = publicationFiles.get()
        require(publications.isNotEmpty()) { "No release publications registered" }
        require(publications.values.toSet().size == publications.size) { "Duplicate Maven publication paths" }
        for ((source, path) in publications) {
            val original = ReleaseArtifacts.resolve(root, source)
            val staged = ReleaseArtifacts.resolve(staging, "maven/$path")
            require(ReleaseArtifacts.sha256(original) == ReleaseArtifacts.sha256(staged)) { "Staged Maven publication differs: $path" }
        }
        MavenMetadata.verifyPreservedVersions(staging)
        for (coordinate in publications.values.map { it.substringBeforeLast('/') }.distinct()) {
            val module = coordinate.substringBeforeLast('/')
            val version = coordinate.substringAfterLast('/')
            val metadata = ReleaseArtifacts.resolve(staging, "maven/$module/maven-metadata.xml")
            require(version in MavenMetadata.versions(metadata)) { "Published Maven version is missing: $coordinate" }
        }
        val files = ReleaseArtifacts.files(staging).sorted()
        require("baseline-metadata.json" in files) { "Missing metadata baseline" }
        staging.resolve("SHA256SUMS").writeText(
            files.joinToString("") { path ->
                "${ReleaseArtifacts.sha256(ReleaseArtifacts.resolve(staging, path))}  $path\n"
            },
        )
        ReleaseArtifacts.verifyStaging(staging)
        logger.lifecycle("Sealed ${publications.size} Maven publication files")
    }
}
