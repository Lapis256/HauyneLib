package dev.lapis256.hauyne.buildlogic.release

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RecordReleaseArtifacts : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Internal
    abstract val localRepository: DirectoryProperty

    @get:Input
    abstract val publicationFiles: MapProperty<String, String>

    @get:InputFiles
    abstract val modFiles: ConfigurableFileCollection

    @TaskAction
    fun record() {
        val root = rootDirectory.get().asFile
        val repository = localRepository.get().asFile
        val publications = publicationFiles.get()
        require(publications.isNotEmpty()) { "No release publications registered" }
        for (mod in modFiles) {
            require(mod.relativeTo(root).invariantSeparatorsPath in publications) {
                "Mod distribution is not a Maven publication artifact: $mod"
            }
        }
        val assets = root.resolve(".release/assets")
        require(!assets.exists()) { "Release assets already exist: $assets" }
        assets.mkdirs()
        val (entries, checksums) = copyVerifiedArtifacts(root, repository, assets)
        writeChecksums(root, entries, checksums)
        logger.lifecycle("Recorded ${checksums.size} publication files")
    }

    private fun copyVerifiedArtifacts(root: File, repository: File, assets: File): Pair<List<String>, List<String>> {
        val entries = mutableListOf<String>()
        val checksums = mutableListOf<String>()
        for ((path, target) in publicationFiles.get().toSortedMap()) {
            val name = target.substringAfterLast('/')
            val file = root.resolve(path)
            require(file.readBytes().contentEquals(repository.resolve(target).readBytes())) {
                "Local Maven publication differs: $path"
            }
            val digest = ReleaseArtifacts.sha256(file)
            file.copyTo(assets.resolve(name))
            entries += "$digest  .release/assets/$name\n"
            checksums += "$digest  $name\n"
        }
        return entries to checksums
    }

    private fun writeChecksums(root: File, entries: List<String>, checksums: List<String>) {
        root.resolve(".release/assets/SHA256SUMS").writeText(checksums.joinToString(""))
        val additionalEntries = listOf("CHANGELOG.md", ".release/notes.md", "gradle.properties", ".release/assets/SHA256SUMS").map {
                path ->
            "${ReleaseArtifacts.sha256(root.resolve(path))}  $path\n"
        }
        val manifest = root.resolve(".release/SHA256SUMS")
        manifest.writeText((entries + additionalEntries).joinToString(""))
    }
}
