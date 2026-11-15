package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

enum class HauyneFmlLoader(val modId: String, val metadataFileName: String) {
    NEOFORGE("neoforge", "neoforge.mods.toml"),
    FORGE("forge", "mods.toml"),
}

abstract class HauyneFmlMetadataExtension {
    abstract val minecraftVersionRange: Property<VersionRange>
    abstract val loaderVersionRange: Property<VersionRange>
    abstract val javaFmlLoaderVersionRange: Property<VersionRange>

    internal abstract val dependencyEntries: ListProperty<HauyneDependency>
}

abstract class HauyneForgeMetadataExtension @Inject constructor(objects: ObjectFactory) : HauyneFmlMetadataExtension() {
    val dependencies = objects.newInstance(HauyneDependencies::class.java)
    internal override val dependencyEntries get() = dependencies.entries

    fun dependencies(action: Action<HauyneDependencies>) = action.execute(dependencies)
}

abstract class HauyneNeoforgeMetadataExtension @Inject constructor(objects: ObjectFactory) : HauyneFmlMetadataExtension() {
    val dependencies = objects.newInstance(HauyneNeoforgeDependencies::class.java)
    internal override val dependencyEntries get() = dependencies.entries

    fun dependencies(action: Action<HauyneNeoforgeDependencies>) = action.execute(dependencies)
}
