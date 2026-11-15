package dev.lapis256.hauyne.buildlogic.library

import org.gradle.api.provider.Property

abstract class HauyneLibraryExtension {
    abstract val componentPath: Property<String>
}
