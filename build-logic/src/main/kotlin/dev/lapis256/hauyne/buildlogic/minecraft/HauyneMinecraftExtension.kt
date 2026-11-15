package dev.lapis256.hauyne.buildlogic.minecraft

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import java.util.Properties
import javax.inject.Inject
import kotlin.reflect.KProperty

abstract class HauyneMinecraftExtension @Inject constructor(providers: ProviderFactory) {
    abstract val minecraftVersion: Property<String>
    abstract val versionPropertiesFile: RegularFileProperty
    private val values = providers.fileContents(versionPropertiesFile).asText.map { text ->
        Properties().apply {
            load(text.reader())
        }
    }

    val configAdapterTarget by values
    val configMinecraftTarget by values

    fun versionProperty(name: String): Provider<String> = values.map { values ->
        requireNotNull(values.getProperty(name)) { "Missing version property: $name" }
    }
}

private operator fun Provider<Properties>.getValue(receiver: Any?, property: KProperty<*>) = this.map {
    requireNotNull(it.getProperty(property.name)) { "Missing version property: ${property.name}" }
}
