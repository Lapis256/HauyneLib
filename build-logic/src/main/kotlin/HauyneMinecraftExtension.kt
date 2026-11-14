import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import java.util.Properties
import javax.inject.Inject

abstract class HauyneMinecraftExtension @Inject constructor(providers: ProviderFactory) {
    abstract val minecraftVersion: Property<String>
    abstract val versionPropertiesFile: RegularFileProperty
    private val values = providers.fileContents(versionPropertiesFile).asText.map {
            text ->
        Properties().apply {
            load(text.reader())
        }
    }

    fun versionProperty(name: String): Provider<String> = values.map {
            values ->
        requireNotNull(values.getProperty(name)) { "Missing version property: $name" }
    }
}
