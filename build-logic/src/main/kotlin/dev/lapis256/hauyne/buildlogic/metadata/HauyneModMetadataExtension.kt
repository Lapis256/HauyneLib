package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.api.provider.Property

abstract class HauyneModMetadataExtension {
    abstract val modId: Property<String>
    abstract val modName: Property<String>
    abstract val modPackage: Property<String>
    abstract val license: Property<String>
    abstract val description: Property<String>
}
