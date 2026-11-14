import org.gradle.api.provider.Property

enum class HauyneModLoader {
    NEOFORGE,
    FORGE,
}

abstract class HauyneModMetadataExtension {
    abstract val modId: Property<String>
    abstract val modName: Property<String>
    abstract val modPackage: Property<String>
    abstract val license: Property<String>
    abstract val loader: Property<HauyneModLoader>
    abstract val minecraftVersionRange: Property<String>
    abstract val loaderVersionRange: Property<String>
    abstract val javaFmlLoaderVersionRange: Property<String>
}
