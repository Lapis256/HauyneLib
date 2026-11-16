package dev.lapis256.hauyne.buildlogic.metadata

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlParser
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MetadataPublicationTest {
    @TempDir
    lateinit var root: File

    @Test
    fun modPluginConnectsDeclarationsToMetadataAndPublicationPlatforms() {
        for (path in listOf("core", "minecraft/test", "forge/test", "neoforge/test")) {
            root.resolve("hauyne-config/$path").mkdirs()
        }
        root.resolve("settings.gradle.kts").writeText(
            """
            plugins { id("dev.kikugie.stonecutter") }
            rootProject.name = "metadata-publication"
            dependencyResolutionManagement {
                versionCatalogs {
                    create("libs") { from(files("${File("../gradle/libs.versions.toml").absoluteFile.invariantSeparatorsPath}")) }
                }
            }
            include(":hauyne-config:core", ":hauyne-config:minecraft:test", ":hauyne-config:forge:test", ":hauyne-config:neoforge:test")
            stonecutter {
                kotlinController = true
                centralScript = "build.gradle.kts"
                create(":hauyne-lib:forge") { version("test", "1.20.1"); vcsVersion = "test" }
                create(":hauyne-lib:neoforge") { version("test", "1.21.1"); vcsVersion = "test" }
            }
            """.trimIndent(),
        )
        root.resolve("gradle.properties").writeText(
            """
            mavenGroup=dev.example
            modId=example
            modPackage=dev.example
            modAuthor=Example
            modName=Example
            modDescription=Example
            license=MIT
            projectUrl=https://example.com
            r2Bucket=example
            releaseChannel=ALPHA
            """.trimIndent(),
        )
        root.resolve("build.gradle.kts").writeText(
            """
            plugins { id("org.jetbrains.dokka"); id("release") }
            version = "1.0"
            subprojects { configurations.create("bcv-rt-jvm-cp") }
            """.trimIndent(),
        )
        for (loader in listOf("forge", "neoforge")) {
            val neoforgeDependencies = if (loader == "neoforge") {
                """
                incompatible("broken_mod") { curseForgeSlug = "curse-broken" }
                discouraged("slow_mod") {
                    modrinthSlug = "modrinth-slow"
                    modrinthRelation = DependencyType.INCOMPATIBLE
                }
                """.trimIndent()
            } else {
                ""
            }
            val controller = root.resolve("hauyne-lib/$loader").apply { mkdirs() }
            controller.resolve("stonecutter.gradle.kts").writeText("plugins { id(\"dev.kikugie.stonecutter\") }\nstonecutter active \"test\"\n")
            controller.resolve("build.gradle.kts").writeText(
                """
                import me.modmuss50.mpp.PlatformDependency.DependencyType
                import dev.lapis256.hauyne.buildlogic.metadata.AnyVersion
                import dev.lapis256.hauyne.buildlogic.metadata.eq
                import dev.lapis256.hauyne.buildlogic.metadata.gte
                import dev.lapis256.hauyne.buildlogic.metadata.rangeUntil
                plugins { id("mod") }
                java { toolchain.languageVersion = JavaLanguageVersion.of(JavaVersion.current().majorVersion.toInt()) }
                hauyneMetadata {
                    $loader {
                        ${if (loader == "neoforge") "minecraftVersionRange = AnyVersion" else ""}
                        loaderVersionRange = "1".gte()
                        javaFmlLoaderVersionRange = "4".eq()
                        dependencies {
                            require("shared_api") {
                                versionRange = "1"..<"2"
                                curseForgeSlug = "curse-api"
                                modrinthSlug = "modrinth-api"
                                modrinthVersionId = "version-id"
                            }
                            optional("curse_only") { curseForgeSlug = "curse-only" }
                            require("modrinth_only") {
                                modrinthSlug = "modrinth-only"
                                modrinthRelation = DependencyType.EMBEDDED
                            }
                            optional("override_mod") {
                                curseForgeSlug = "curse-override"
                                curseForgeRelation = DependencyType.INCOMPATIBLE
                            }
                            require("local_only")
                            $neoforgeDependencies
                        }
                    }
                }
                publishMods {
                    curseforge {
                        check(dependencies.get().associate { it.slug.get() to it.type.get() } == mapOf(
                            "kotlinlangforge" to DependencyType.REQUIRED,
                            "curse-api" to DependencyType.REQUIRED,
                            "curse-only" to DependencyType.OPTIONAL,
                            "curse-override" to DependencyType.INCOMPATIBLE,
                        ) + if ("$loader" == "neoforge") mapOf("curse-broken" to DependencyType.INCOMPATIBLE) else emptyMap())
                    }
                    modrinth {
                        check(dependencies.get().associate { it.slug.get() to (it.type.get() to it.version.orNull) } == mapOf(
                            "kotlin-lang-forge" to (DependencyType.REQUIRED to null),
                            "modrinth-api" to (DependencyType.REQUIRED to "version-id"),
                            "modrinth-only" to (DependencyType.EMBEDDED to null),
                        ) + if ("$loader" == "neoforge") mapOf("modrinth-slow" to (DependencyType.INCOMPATIBLE to null)) else emptyMap())
                    }
                }
                """.trimIndent(),
            )
        }
        val runner = GradleRunner.create().withProjectDir(root).withPluginClasspath()
            .withArguments(
                ":hauyne-lib:forge:test:generateModMetadata",
                ":hauyne-lib:neoforge:test:generateModMetadata",
                "--configuration-cache",
                "--offline",
                "--console=plain",
            )
        runner.build()
        runner.build()
        for (loader in HauyneFmlLoader.entries) {
            val file = root.resolve("hauyne-lib/${loader.modId}/versions/test/build/generated/resources/modMetadata/${loader.metadataFileName}")
            val metadata = TomlParser().parse(file.readText())
            assertEquals("javafml", metadata.get<String>("modLoader"))
            assertEquals("[4]", metadata.get<String>("loaderVersion"))
            val dependencies = metadata.get<List<Config>>("dependencies.example").associateBy { it.get<String>("modId") }
            assertEquals(
                setOf("minecraft", loader.modId, "shared_api", "curse_only", "modrinth_only", "override_mod", "local_only") +
                    if (loader == HauyneFmlLoader.NEOFORGE) setOf("broken_mod", "slow_mod") else emptySet(),
                dependencies.keys,
            )
            assertEquals(
                if (loader == HauyneFmlLoader.FORGE) "[1.20.1]" else "(,)",
                dependencies.getValue("minecraft").get<String>("versionRange"),
            )
            assertEquals("[1,)", dependencies.getValue(loader.modId).get<String>("versionRange"))
            assertEquals("[1,2)", dependencies.getValue("shared_api").get<String>("versionRange"))
            assertEquals("(,)", dependencies.getValue("local_only").get<String>("versionRange"))
        }
    }
}
