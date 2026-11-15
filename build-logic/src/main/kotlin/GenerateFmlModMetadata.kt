import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import com.electronwill.nightconfig.toml.TomlWriter
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
        val metadata = table(
            "modLoader" to "javafml",
            "loaderVersion" to javaFmlLoaderVersionRange.get(),
            "license" to license.get(),
            "mods" to listOf(
                table(
                    "modId" to id,
                    "version" to modVersion.get(),
                    "displayName" to modName.get(),
                    "description" to modDescription.get(),
                ),
            ),
            "dependencies" to table(id to dependencies),
        )
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        val writer = TomlWriter().apply {
            setNewline("\n")
            setWriteStringMultilinePredicate { false }
        }
        file.writeText(writer.writeToString(metadata), Charsets.UTF_8)
    }

    private fun dependency(id: String, range: String): Config {
        val requirement = if (loader.get() == HauyneFmlLoader.FORGE) "mandatory" to true else "type" to "required"
        return table("modId" to id, "versionRange" to range, requirement, "ordering" to "NONE", "side" to "BOTH")
    }

    private fun table(vararg entries: Pair<String, Any>): Config = Config.wrap(linkedMapOf(*entries), TomlFormat.instance())
}
