package dev.lapis256.hauyne.buildlogic.metadata

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlOutputConfig
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateFmlModMetadata : DefaultTask() {
    @get:Input
    abstract val modId: Property<String>

    @get:Input
    abstract val modName: Property<String>

    @get:Input
    abstract val modAuthor: Property<String>

    @get:Input
    abstract val issueTrackerUrl: Property<String>

    @get:Input
    abstract val displayUrl: Property<String>

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

    @get:Input
    @get:Optional
    abstract val kotlinVersionRange: Property<String>

    @get:Nested
    abstract val modDependencies: ListProperty<HauyneDependency>

    init {
        modDependencies.convention(emptyList())
    }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val id = modId.get()
        require(id.matches(Regex("[a-z][a-z0-9_]{1,63}"))) { "Invalid FML mod ID: $id" }
        val declared = modDependencies.get()
        validateDependencies(declared, id, loader.get())
        val dependencies = listOf(
            dependency("minecraft", minecraftVersionRange.get()),
            dependency(loader.get().modId, loaderVersionRange.get()),
        ) + declared.map {
            FmlDependency(
                modId = it.modId,
                versionRange = it.versionRange.get().versionRange,
                mandatory = if (loader.get() == HauyneFmlLoader.FORGE) it.type == DependencyKind.REQUIRED else null,
                type = if (loader.get() == HauyneFmlLoader.NEOFORGE) it.type.name.lowercase() else null,
                ordering = it.ordering.get().name,
                side = it.side.get().name,
                reason = if (loader.get() == HauyneFmlLoader.NEOFORGE) it.reason.orNull else null,
            )
        }
        val metadata = FmlModMetadata(
            loaderVersion = javaFmlLoaderVersionRange.get(),
            license = license.get(),
            issueTrackerURL = issueTrackerUrl.get(),
            mods = listOf(FmlMod(id, modVersion.get(), modName.get(), modDescription.get(), modAuthor.get(), displayUrl.get())),
            dependencies = mapOf(id to dependencies),
            features = kotlinVersionRange.orNull?.let { mapOf(id to mapOf("hauyneKotlinVersion" to it)) },
        )
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        val toml = Toml(outputConfig = TomlOutputConfig.compliant())
        val comments = if (loader.get() == HauyneFmlLoader.FORGE) {
            declared.filter { it.reason.isPresent }.joinToString("") { dependency ->
                dependency.reason.get().lineSequence().joinToString("\n", postfix = "\n") { "# ${dependency.modId}: $it" }
            }
        } else {
            ""
        }
        file.writeText(comments + toml.encodeToString(metadata), Charsets.UTF_8)
    }

    private fun dependency(id: String, range: String): FmlDependency = if (loader.get() == HauyneFmlLoader.FORGE) {
        FmlDependency(id, range, mandatory = true)
    } else {
        FmlDependency(id, range, type = "required")
    }
}
