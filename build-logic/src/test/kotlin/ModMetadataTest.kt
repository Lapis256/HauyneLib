import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlParser
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModMetadataTest {
    @TempDir
    lateinit var root: File

    private fun task(loader: HauyneFmlLoader): GenerateFmlModMetadata = ProjectBuilder.builder().withProjectDir(root).build().tasks
        .register("generateMetadata", GenerateFmlModMetadata::class.java).get().apply {
            modId.set("hauynelib")
            modName.set("HauyneLib")
            modVersion.set("26.1-0.1.0")
            license.set("MIT")
            modDescription.set("Configuration library")
            this.loader.set(loader)
            minecraftVersionRange.set("[26.1.2]")
            loaderVersionRange.set("[1,)")
            javaFmlLoaderVersionRange.set("[4,)")
            outputFile.set(root.resolve(loader.metadataFileName))
        }

    @Test
    fun requiredDependenciesUseEachLoadersSchema() {
        for (loader in HauyneFmlLoader.entries) {
            val task = task(loader)
            task.generate()
            val metadata = TomlParser().parse(task.outputFile.get().asFile.readText())
            assertEquals("javafml", metadata.get<String>("modLoader"))
            assertEquals("[4,)", metadata.get<String>("loaderVersion"))
            assertEquals("MIT", metadata.get<String>("license"))
            val mod = metadata.get<List<Config>>("mods").single()
            assertEquals("hauynelib", mod.get<String>("modId"))
            assertEquals("26.1-0.1.0", mod.get<String>("version"))
            val dependencies = metadata.get<List<Config>>("dependencies.hauynelib").associateBy { it.get<String>("modId") }
            assertEquals(setOf("minecraft", loader.modId), dependencies.keys)
            assertEquals("[26.1.2]", dependencies.getValue("minecraft").get<String>("versionRange"))
            assertEquals("[1,)", dependencies.getValue(loader.modId).get<String>("versionRange"))
            for (dependency in dependencies.values) {
                if (loader == HauyneFmlLoader.FORGE) {
                    assertEquals(true, dependency.get<Boolean>("mandatory"))
                    assertFalse(dependency.contains("type"))
                } else {
                    assertEquals("required", dependency.get<String>("type"))
                    assertFalse(dependency.contains("mandatory"))
                }
            }
        }
    }

    @Test
    fun quotesNewlinesAndTemplateMarkersRemainLiteral() {
        val task = task(HauyneFmlLoader.NEOFORGE)
        val text = "日本語 \"quoted\" \\ path\nnext\t\${literal}"
        task.modName.set(text)
        task.modDescription.set(text)
        task.generate()
        val metadata = TomlParser().parse(task.outputFile.get().asFile.readText())
        val mod = metadata.get<List<Config>>("mods").single()
        assertEquals(text, mod.get<String>("displayName"))
        assertEquals(text, mod.get<String>("description"))
    }

    @Test
    fun invalidModIdIsRejected() {
        val task = task(HauyneFmlLoader.FORGE)
        task.modId.set("invalid.mod")
        assertThrows(IllegalArgumentException::class.java) { task.generate() }
    }
}
