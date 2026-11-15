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

    val forgeLike = objects.newInstance(HauyneFmlMetadataExtension::class.java)

    fun forgeLike(action: Action<HauyneFmlMetadataExtension>) = action.execute(forgeLike)
}
