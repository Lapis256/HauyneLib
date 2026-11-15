package dev.lapis256.hauyne.buildlogic.metadata

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlOutputConfig
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateFmlModMetadata : DefaultTask() {
    @get:Input
    abstract val modId: Property<String>

    @get:Input
    abstract val modName: Property<String>

    @get:Input
    abstract val modVersion: Property<String>

    @get:Input
    abstract val license: Property<String>

    @get:Input
    abstract val modDescription: Property<String>

    @get:Input
    abstract val loader: Property<HauyneFmlLoader>

    @get:Input
    abstract val minecraftVersionRange: Property<String>

    @get:Input
    abstract val loaderVersionRange: Property<String>

    @get:Input
    abstract val javaFmlLoaderVersionRange: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val id = modId.get()
        require(id.matches(Regex("[a-z][a-z0-9_]{1,63}"))) { "Invalid FML mod ID: $id" }
        val dependencies = listOf(
            dependency("minecraft", minecraftVersionRange.get()),
            dependency(loader.get().modId, loaderVersionRange.get()),
        )
        val metadata = FmlModMetadata(
            loaderVersion = javaFmlLoaderVersionRange.get(),
            license = license.get(),
            mods = listOf(FmlMod(id, modVersion.get(), modName.get(), modDescription.get())),
            dependencies = mapOf(id to dependencies),
        )
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        val toml = Toml(outputConfig = TomlOutputConfig.compliant())
        file.writeText(toml.encodeToString(metadata), Charsets.UTF_8)
    }

    private fun dependency(id: String, range: String): FmlDependency = if (loader.get() == HauyneFmlLoader.FORGE) {
        FmlDependency(id, range, mandatory = true)
    } else {
        FmlDependency(id, range, type = "required")
    }
}
