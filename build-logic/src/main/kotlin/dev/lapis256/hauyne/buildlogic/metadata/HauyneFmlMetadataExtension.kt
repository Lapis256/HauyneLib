package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.api.provider.Property

enum class HauyneFmlLoader(val modId: String, val metadataFileName: String) {
    NEOFORGE("neoforge", "neoforge.mods.toml"),
    FORGE("forge", "mods.toml"),
}

abstract class HauyneFmlMetadataExtension {
    abstract val loader: Property<HauyneFmlLoader>
    abstract val minecraftVersionRange: Property<String>
    abstract val loaderVersionRange: Property<String>
    abstract val javaFmlLoaderVersionRange: Property<String>
}
