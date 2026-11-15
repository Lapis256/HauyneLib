package dev.lapis256.hauyne.buildlogic.metadata

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlParser
import me.modmuss50.mpp.PlatformDependency.DependencyType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun loaderBlocksSelectTheirOwnMetadataAndCannotBeMixed() {
        for (loader in HauyneFmlLoader.entries) {
            val metadata = ProjectBuilder.builder().withProjectDir(root).build().objects
                .newInstance(HauyneModMetadataExtension::class.java)
            assertFalse(metadata.selectedLoader.isPresent)
            when (loader) {
                HauyneFmlLoader.FORGE -> metadata.forge {
                    minecraftVersionRange.set("[1.20.1]")
                    dependencies { require("forge_api") }
                }

                HauyneFmlLoader.NEOFORGE -> metadata.neoforge {
                    minecraftVersionRange.set("[1.21.1]")
                    dependencies { require("neoforge_api") }
                }
            }
            assertEquals(loader, metadata.selectedLoader.get())
            val task = task(loader)
            task.loader.set(metadata.selectedLoader)
            task.minecraftVersionRange.set(metadata.selectedMetadata.flatMap { it.minecraftVersionRange })
            task.modDependencies.set(metadata.selectedMetadata.flatMap { it.dependencyEntries })
            task.generate()
            val dependencies = TomlParser().parse(task.outputFile.get().asFile.readText())
                .get<List<Config>>("dependencies.hauynelib").associateBy { it.get<String>("modId") }
            assertTrue(dependencies.containsKey("${loader.modId}_api"))
            val expectedRange = if (loader == HauyneFmlLoader.FORGE) "[1.20.1]" else "[1.21.1]"
            assertEquals(expectedRange, dependencies.getValue("minecraft").get<String>("versionRange"))
            assertThrows(IllegalArgumentException::class.java) {
                when (loader) {
                    HauyneFmlLoader.FORGE -> metadata.neoforge {}
                    HauyneFmlLoader.NEOFORGE -> metadata.forge {}
                }
            }
        }
    }

    private fun declarations(): HauyneNeoforgeDependencies = ProjectBuilder.builder().withProjectDir(root).build()
        .objects.newInstance(HauyneNeoforgeDependencies::class.java)

    @Test
    fun declaredDependenciesPreserveLoaderSemanticsAndEscapeReasons() {
        for (loader in HauyneFmlLoader.entries) {
            val declarations = declarations()
            declarations.require("example_api") {
                versionRange.set("[1,2)")
                reason.set("First line\r\n[[mods]]\n# reason")
                ordering.set(HauyneDependencyOrdering.AFTER)
                side.set(HauyneDependencySide.CLIENT)
            }
            declarations.optional("integration")
            if (loader == HauyneFmlLoader.NEOFORGE) {
                declarations.incompatible("broken_mod")
                declarations.discouraged("slow_mod")
            }
            val task = task(loader)
            task.modDependencies.set(declarations.entries)
            task.generate()
            val text = task.outputFile.get().asFile.readText()
            val metadata = TomlParser().parse(text)
            assertEquals(1, metadata.get<List<Config>>("mods").size)
            val dependencies = metadata.get<List<Config>>("dependencies.hauynelib").associateBy { it.get<String>("modId") }
            val api = dependencies.getValue("example_api")
            assertEquals("[1,2)", api.get<String>("versionRange"))
            assertEquals("AFTER", api.get<String>("ordering"))
            assertEquals("CLIENT", api.get<String>("side"))
            assertEquals("(,)", dependencies.getValue("integration").get<String>("versionRange"))
            if (loader == HauyneFmlLoader.FORGE) {
                assertEquals(true, api.get<Boolean>("mandatory"))
                assertEquals(false, dependencies.getValue("integration").get<Boolean>("mandatory"))
                assertFalse(api.contains("reason"))
                assertTrue(text.contains("# example_api: [[mods]]"))
            } else {
                assertEquals("required", api.get<String>("type"))
                assertEquals("optional", dependencies.getValue("integration").get<String>("type"))
                assertEquals("incompatible", dependencies.getValue("broken_mod").get<String>("type"))
                assertEquals("discouraged", dependencies.getValue("slow_mod").get<String>("type"))
                assertEquals("First line\r\n[[mods]]\n# reason", api.get<String>("reason"))
            }
        }
    }

    @Test
    fun invalidDeclarationsAreRejectedBeforeWritingMetadata() {
        val invalid: List<(HauyneNeoforgeDependencies) -> Unit> = listOf(
            {
                it.require("duplicate")
                it.optional("duplicate")
            },
            { it.require("hauynelib") },
            { it.require("minecraft") },
            { it.require("forge") },
            { it.require("INVALID") },
            { it.incompatible("broken_mod") },
            { it.discouraged("slow_mod") },
            { it.require("example_api") { versionRange.set("") } },
            { it.require("example_api") { curseForgeSlug.set(" ") } },
            { it.require("example_api") { modrinthSlug.set("") } },
            { it.require("example_api") { curseForgeRelation.set(DependencyType.OPTIONAL) } },
            { it.require("example_api") { modrinthRelation.set(DependencyType.EMBEDDED) } },
            { it.require("example_api") { modrinthVersionId.set("abcdefgh") } },
            {
                it.require("example_api") {
                    modrinthSlug.set("example-api")
                    modrinthVersionId.set("")
                }
            },
        )
        for (configure in invalid) {
            val declarations = declarations()
            configure(declarations)
            val task = task(HauyneFmlLoader.FORGE)
            task.modDependencies.set(declarations.entries)
            assertThrows(IllegalArgumentException::class.java) { task.generate() }
            assertFalse(task.outputFile.get().asFile.exists())
        }
    }

    @Test
    fun publicationRelationsFollowDeclarationsAndAllowExplicitOverrides() {
        val declarations = declarations()
        declarations.require("required_mod") { curseForgeSlug.set("required-mod") }
        declarations.optional("optional_mod") { modrinthSlug.set("optional-mod") }
        declarations.incompatible("incompatible_mod")
        declarations.discouraged("discouraged_mod") {
            modrinthSlug.set("discouraged-mod")
            modrinthRelation.set(DependencyType.INCOMPATIBLE)
        }
        val entries = declarations.entries.get()
        validateDependencies(entries, "hauynelib", HauyneFmlLoader.NEOFORGE)
        assertEquals(
            listOf(DependencyType.REQUIRED, DependencyType.OPTIONAL, DependencyType.INCOMPATIBLE, DependencyType.INCOMPATIBLE),
            entries.map { it.relation(it.modrinthRelation) },
        )
        entries.last().modrinthRelation.unset()
        assertThrows(IllegalArgumentException::class.java) {
            validateDependencies(entries, "hauynelib", HauyneFmlLoader.NEOFORGE)
        }
    }
}
