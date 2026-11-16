package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class HauyneModMetadataExtension @Inject constructor(objects: ObjectFactory) {
    abstract val modId: Property<String>
    abstract val modName: Property<String>
    abstract val modAuthor: Property<String>
    abstract val issueTrackerUrl: Property<String>
    abstract val displayUrl: Property<String>
    abstract val modPackage: Property<String>
    abstract val license: Property<String>
    abstract val description: Property<String>

    private val forgeMetadata = objects.newInstance(HauyneForgeMetadataExtension::class.java)
    private val neoforgeMetadata = objects.newInstance(HauyneNeoforgeMetadataExtension::class.java)
    internal val selectedLoader: Property<HauyneFmlLoader> = objects.property(HauyneFmlLoader::class.java)
    internal val selectedMetadata = selectedLoader.map {
        when (it) {
            HauyneFmlLoader.FORGE -> forgeMetadata
            HauyneFmlLoader.NEOFORGE -> neoforgeMetadata
        }
    }

    fun forge(action: Action<HauyneForgeMetadataExtension>) = configure(HauyneFmlLoader.FORGE, forgeMetadata, action)

    fun neoforge(action: Action<HauyneNeoforgeMetadataExtension>) = configure(HauyneFmlLoader.NEOFORGE, neoforgeMetadata, action)

    private fun <T : HauyneFmlMetadataExtension> configure(loader: HauyneFmlLoader, metadata: T, action: Action<T>) {
        require(!selectedLoader.isPresent || selectedLoader.get() == loader) {
            "Configure only one of hauyneMetadata.forge {} or hauyneMetadata.neoforge {} per project"
        }
        selectedLoader.set(loader)
        action.execute(metadata)
    }
}
