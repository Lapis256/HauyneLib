package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

enum class HauyneFmlLoader(val modId: String, val metadataFileName: String) {
    NEOFORGE("neoforge", "neoforge.mods.toml"),
    FORGE("forge", "mods.toml"),
}

abstract class HauyneFmlMetadataExtension @Inject constructor(objects: ObjectFactory) {
    abstract val minecraftVersionRange: Property<String>
    abstract val loaderVersionRange: Property<String>
    abstract val javaFmlLoaderVersionRange: Property<String>

    val dependencies = objects.newInstance(HauyneDependencies::class.java)

    fun dependencies(action: Action<HauyneDependencies>) = action.execute(dependencies)
}
