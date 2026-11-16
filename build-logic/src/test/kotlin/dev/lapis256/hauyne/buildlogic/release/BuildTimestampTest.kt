package dev.lapis256.hauyne.buildlogic.release

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.OffsetDateTime
import java.util.jar.JarFile

class BuildTimestampTest {
    @TempDir
    lateinit var root: File

    @Test
    fun preservesBuildTimeAcrossSeparateConfigurations() {
        root.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "timestamp-test"
            dependencyResolutionManagement {
                versionCatalogs {
                    create("libs") { from(files("${File("../gradle/libs.versions.toml").absoluteFile.invariantSeparatorsPath}")) }
                }
            }
            """.trimIndent(),
        )
        root.resolve("gradle.properties").writeText(
            """
            mavenGroup=dev.example
            modId=example
            modName=Example
            modAuthor=Example
            projectUrl=https://example.com
            """.trimIndent(),
        )
        root.resolve("build.gradle.kts").writeText(
            """
            plugins { id("library") apply false }
            configurations.create("bcv-rt-jvm-cp")
            apply(plugin = "library")
            version = "1.0"
            extensions.configure<JavaPluginExtension> {
                toolchain.languageVersion = JavaLanguageVersion.of(JavaVersion.current().majorVersion.toInt())
            }
            """.trimIndent(),
        )
        val runner = GradleRunner.create().withProjectDir(root).withPluginClasspath()
        fun build(timestamp: String): ByteArray {
            runner.withArguments("jar", "-PbuildTimestamp=$timestamp", "--no-configuration-cache", "--offline").build()
            val jar = root.resolve("build/libs").listFiles()!!.single { it.extension == "jar" }
            JarFile(jar).use {
                val attributes = it.manifest.mainAttributes
                assertEquals(OffsetDateTime.parse(timestamp).toInstant().toEpochMilli().toString(), attributes.getValue("Timestamp"))
                assertEquals(timestamp.replace("Z", "+0000"), attributes.getValue("Implementation-Timestamp"))
            }
            return jar.readBytes()
        }
        val first = build("2026-11-17T00:00:00Z")
        assertArrayEquals(first, build("2026-11-17T00:00:00Z"))
        assertFalse(first.contentEquals(build("2026-11-17T00:00:01Z")))
    }
}
