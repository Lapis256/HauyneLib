package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class HauyneModMetadataExtension @Inject constructor(objects: ObjectFactory) {
    abstract val modId: Property<String>
    abstract val modName: Property<String>
    abstract val modPackage: Property<String>
    abstract val license: Property<String>
    abstract val description: Property<String>

    private val forgeMetadata = objects.newInstance(HauyneFmlMetadataExtension::class.java)
    private val neoforgeMetadata = objects.newInstance(HauyneFmlMetadataExtension::class.java)
    internal val selectedLoader: Property<HauyneFmlLoader> = objects.property(HauyneFmlLoader::class.java)
    internal val selectedMetadata = selectedLoader.map {
        when (it) {
            HauyneFmlLoader.FORGE -> forgeMetadata
            HauyneFmlLoader.NEOFORGE -> neoforgeMetadata
        }
    }

    fun forge(action: Action<HauyneFmlMetadataExtension>) = configure(HauyneFmlLoader.FORGE, forgeMetadata, action)

    fun neoforge(action: Action<HauyneFmlMetadataExtension>) = configure(HauyneFmlLoader.NEOFORGE, neoforgeMetadata, action)

    private fun configure(loader: HauyneFmlLoader, metadata: HauyneFmlMetadataExtension, action: Action<HauyneFmlMetadataExtension>) {
        require(!selectedLoader.isPresent || selectedLoader.get() == loader) {
            "Configure only one of hauyneMetadata.forge {} or hauyneMetadata.neoforge {} per project"
        }
        selectedLoader.set(loader)
        action.execute(metadata)
    }
}
