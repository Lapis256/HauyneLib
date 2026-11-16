package dev.lapis256.hauyne.buildlogic.server

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties

class DevServerPluginTest {
    @TempDir
    lateinit var root: File

    @Test
    fun serverRunInitializesMissingFilesAndPreservesUserChanges() {
        root.resolve("settings.gradle.kts").writeText("rootProject.name = \"dev-server-test\"\n")
        root.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("dev-server")
                id("net.neoforged.moddev")
            }
            neoForge {
                runs {
                    create("custom") {
                        server()
                        gameDirectory = layout.projectDirectory.dir("custom-server")
                    }
                    create("client") { client() }
                    create("data") {
                        serverData()
                        gameDirectory = layout.projectDirectory.dir("custom-data")
                    }
                }
            }
            tasks.register("prepareDataRun")
            afterEvaluate {
                check("--nogui" in neoForge.runs["custom"].programArguments.get())
                check("--nogui" in neoForge.runs["data"].programArguments.get())
                check("--nogui" !in neoForge.runs["client"].programArguments.get())
                check(tasks.findByName("setupClientDevServer") == null)
            }
            tasks.register("prepareCustomRun") {
                val directory = layout.projectDirectory.dir("custom-server").asFile
                doLast {
                    check(directory.resolve("eula.txt").exists())
                    check(directory.resolve("server.properties").exists())
                }
            }
            """.trimIndent(),
        )
        val runner = GradleRunner.create().withProjectDir(root).withPluginClasspath()
            .withArguments("prepareCustomRun", "--offline", "--configuration-cache", "--console=plain")
        runner.build()
        runner.withArguments("prepareDataRun", "--offline", "--configuration-cache", "--console=plain").build()
        assertTrue(root.resolve("custom-data/eula.txt").exists())
        runner.withArguments("prepareCustomRun", "--offline", "--configuration-cache", "--console=plain")
        val eula = root.resolve("custom-server/eula.txt")
        val settings = root.resolve("custom-server/server.properties")
        assertEquals("true", Properties().apply { eula.inputStream().use { load(it) } }.getProperty("eula"))
        val properties = Properties().apply { settings.inputStream().use { load(it) } }
        assertEquals("127.0.0.1", properties.getProperty("server-ip"))
        assertEquals("minecraft:flat", properties.getProperty("level-type"))
        assertTrue(properties.getProperty("generator-settings").contains("minecraft:the_void"))
        settings.writeText("gamemode=survival\n")
        eula.writeText("eula=false\n")
        runner.build()
        assertEquals("gamemode=survival\n", settings.readText())
        assertEquals("eula=false\n", eula.readText())
        eula.delete()
        runner.build()
        assertTrue(eula.exists())
        assertEquals("gamemode=survival\n", settings.readText())
    }
}
