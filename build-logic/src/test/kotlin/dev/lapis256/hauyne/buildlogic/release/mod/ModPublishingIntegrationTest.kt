package dev.lapis256.hauyne.buildlogic.release.mod

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModPublishingIntegrationTest {
    @TempDir
    lateinit var root: File

    private fun fixture() {
        root.resolve("settings.gradle.kts").writeText("rootProject.name = \"mod-publication\"\ninclude(\"forge\", \"neoforge\")\n")
        root.resolve("build.gradle.kts").writeText(
            """
            plugins {
                base
                id("me.modmuss50.mod-publish-plugin") apply false
                id("net.neoforged.moddev.legacyforge") apply false
                id("net.neoforged.moddev") apply false
            }
            tasks.register("publishMods") {
                dependsOn(":forge:publishMods", ":neoforge:publishMods")
            }
            """.trimIndent(),
        )
        for (loader in listOf("forge", "neoforge")) {
            val directory = root.resolve(loader).apply { mkdirs() }
            val loaderPlugin = if (loader == "forge") "net.neoforged.moddev.legacyforge" else "net.neoforged.moddev"
            val loaderConfig = if (loader == "forge") "legacyForge { version = \"1.20.1-47.4.0\" }" else "neoForge { version = \"21.1.172\" }"
            directory.resolve("build.gradle.kts").writeText(
                """
                import dev.lapis256.hauyne.buildlogic.release.mod.ModPublishing
                import me.modmuss50.mpp.PublishModTask
                plugins {
                    `java-library`
                    `maven-publish`
                    id("me.modmuss50.mod-publish-plugin")
                    id("$loaderPlugin")
                }
                $loaderConfig
                java { toolchain.languageVersion = JavaLanguageVersion.of(JavaVersion.current().majorVersion.toInt()) }
                publishing.publications.create<MavenPublication>("maven") { from(components["java"]) }
                afterEvaluate {
                    publishing.publications.named<MavenPublication>("maven") {
                        groupId = "dev.example"
                        artifactId = "mod-$loader"
                        version = "1.20.1-1.0"
                    }
                }
                publishMods {
                    version = "1.20.1-1.0-$loader"
                    displayName = file.map { it.asFile.nameWithoutExtension }
                    changelog = "notes"
                    type = STABLE
                    dryRun = true
                    modLoaders.add("$loader")
                    modrinth { projectId = "ABCD1234"; minecraftVersions.add("1.20.1") }
                    curseforge { projectId = "42"; minecraftVersions.add("1.20.1"); client = true; server = true }
                }
                gradle.projectsEvaluated {
                    val publication = project.extensions.getByType<PublishingExtension>().publications.getByName("maven") as MavenPublication
                    publishMods.file.fileValue(ModPublishing.stagedJar(rootProject, publication))
                }
                tasks.register("checkContract") {
                    doLast {
                        val publication = publishing.publications.getByName("maven") as MavenPublication
                        val main = publication.artifacts.single { it.extension == "jar" && it.classifier.isNullOrEmpty() }
                        val finalJar = tasks.named<AbstractArchiveTask>("${if (loader == "forge") "reobfJar" else "jar"}").get()
                        check(main.file == finalJar.archiveFile.get().asFile) { "Maven must publish the final distribution JAR: " + main.file }
                        check(publishMods.file.get().asFile == rootProject.file("build/staging/maven/dev/example/mod-$loader/1.20.1-1.0/mod-$loader-1.20.1-1.0.jar"))
                        check(publishMods.file.get().asFile != finalJar.archiveFile.get().asFile)
                        tasks.withType<PublishModTask>().forEach {
                            check(it.platform.file.get().asFile == publishMods.file.get().asFile)
                        }
                        println("MOD_CONTRACT_OK_$loader")
                    }
                }
                """.trimIndent(),
            )
            val staged = root.resolve("build/staging/maven/dev/example/mod-$loader/1.20.1-1.0/mod-$loader-1.20.1-1.0.jar")
            staged.parentFile.mkdirs()
            staged.writeText("staged $loader JAR")
        }
    }

    private fun runner(vararg tasks: String) = GradleRunner.create().withProjectDir(root).withPluginClasspath()
        .withArguments(*tasks, "--offline", "--console=plain", "--no-configuration-cache", "--stacktrace")

    @Test
    fun rootPublishGraphHasOnlyPlatformSendTasksAndMavenUsesFinalJars() {
        fixture()
        val contracts = runner(":forge:checkContract", ":neoforge:checkContract").build().output
        assertTrue(contracts.contains("MOD_CONTRACT_OK_forge"), contracts)
        assertTrue(contracts.contains("MOD_CONTRACT_OK_neoforge"), contracts)
        val graph = runner("publishMods", "--dry-run").build().output
        for (loader in listOf("forge", "neoforge")) {
            assertTrue(graph.contains(":$loader:publishModrinth SKIPPED"), graph)
            assertTrue(graph.contains(":$loader:publishCurseforge SKIPPED"), graph)
        }
        assertFalse(
            graph.lineSequence().any {
                it.startsWith(":") &&
                    Regex("(?i)(:jar |reobfJar |compile|stageRelease |ToStagingRepository|createMinecraftArtifacts)").containsMatchIn(it)
            },
            graph,
        )
    }
}
