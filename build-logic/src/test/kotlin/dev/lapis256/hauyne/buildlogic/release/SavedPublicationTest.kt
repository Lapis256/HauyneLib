package dev.lapis256.hauyne.buildlogic.release

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SavedPublicationTest {
    @TempDir
    lateinit var root: File

    @Test
    fun publishesSavedFilesWithoutBuildOutputsAndRejectsChangedFiles() {
        root.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "saved-publication"
            include(":lib")
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
            license=MIT
            r2Bucket=example
            """.trimIndent(),
        )
        root.resolve("build.gradle.kts").writeText(
            """
            plugins { id("release"); id("org.jetbrains.dokka") }
            version = "1.0"
            subprojects { configurations.create("bcv-rt-jvm-cp") }
            """.trimIndent(),
        )
        val library = root.resolve("lib").apply { mkdirs() }
        val script = library.resolve("build.gradle.kts").apply {
            writeText(
                """
                plugins { id("library"); id("maven-publishing") }
                java { toolchain.languageVersion = JavaLanguageVersion.of(JavaVersion.current().majorVersion.toInt()) }
                publishing.repositories.remove(publishing.repositories.getByName("R2"))
                publishing.repositories.maven {
                    name = "Test"
                    url = rootProject.layout.projectDirectory.dir("published").asFile.toURI()
                }
                tasks.withType<PublishToMavenRepository>().matching { it.name.endsWith("ToR2Repository") }.configureEach {
                    enabled = false
                }
                """.trimIndent(),
            )
        }
        library.resolve("src/main/resources/example.txt").apply {
            parentFile.mkdirs()
            writeText("Verified resource\n")
        }
        root.resolve(".release").mkdirs()
        root.resolve(".release/notes.md").writeText("Release notes")
        root.resolve("CHANGELOG.md").writeText("Changelog")
        val runner = GradleRunner.create().withProjectDir(root).withPluginClasspath()
        val common = listOf("--offline", "--no-configuration-cache", "-Dmaven.repo.local=${root.resolve("local-maven")}")
        runner.withArguments(common + "recordReleaseArtifacts").build()
        val assets = root.resolve(".release/assets").listFiles()!!.filter { it.name != "SHA256SUMS" }
            .associate { it.name to it.readBytes() }
        library.resolve("build").deleteRecursively()
        script.appendText("\ntasks.withType<Jar>().configureEach { doFirst { error(\"Must not rebuild\") } }\n")
        val publish = common + listOf("publishRelease", "-PreleaseManifest=.release/SHA256SUMS", "-Dorg.gradle.s3.endpoint=unused")
        val result = runner.withArguments(publish).build()
        assertFalse(result.tasks.any { it.path.endsWith(":jar") || it.path.endsWith(":sourcesJar") || "compile" in it.path })
        for ((name, expected) in assets) {
            assertArrayEquals(expected, root.resolve("published/dev/example/lib/1.0/$name").readBytes())
        }
        root.resolve(".release/assets/lib-1.0.module").appendText("changed")
        assertTrue(runner.withArguments(publish).buildAndFail().output.contains("Release artifact changed"))
        assertTrue(runner.withArguments(common + "publishRelease").buildAndFail().output.contains("releaseManifest is required"))
    }
}
