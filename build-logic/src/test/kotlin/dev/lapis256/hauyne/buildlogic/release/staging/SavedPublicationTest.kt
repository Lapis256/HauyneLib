package dev.lapis256.hauyne.buildlogic.release.staging

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
    fun stagesSharedCoordinatesAndConsumesSavedFilesWithoutBuildOutputs() {
        root.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "saved-publication"
            include(":first", ":second")
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
            tasks.named("downloadMavenMetadata") { enabled = false }
            tasks.register("consumeStaging") {
                doLast { file("build/staging/maven").copyRecursively(file("published")) }
            }
            """.trimIndent(),
        )
        for ((name, version) in listOf("first" to "1.0", "second" to "2.0")) {
            val library = root.resolve(name).apply { mkdirs() }
            library.resolve("build.gradle.kts").writeText(
                """
                plugins { id("library"); id("maven-publishing") }
                java { toolchain.languageVersion = JavaLanguageVersion.of(JavaVersion.current().majorVersion.toInt()) }
                base.archivesName = "lib"
                version = "$version"
                """.trimIndent(),
            )
            library.resolve("src/main/resources/example.txt").apply {
                parentFile.mkdirs()
                writeText("Verified resource $version\n")
            }
        }
        val staging = root.resolve("build/staging").apply { mkdirs() }
        staging.resolve("baseline-metadata.json").writeText("{}\n")
        staging.resolve("maven/dev/example/lib/maven-metadata.xml").apply {
            parentFile.mkdirs()
            writeText(
                """<metadata><groupId>dev.example</groupId><artifactId>lib</artifactId><versioning><versions><version>0.5</version></versions></versioning></metadata>""",
            )
        }
        val runner = GradleRunner.create().withProjectDir(root).withPluginClasspath()
        val common = listOf("--offline", "--no-configuration-cache", "--parallel")
        runner.withArguments(common + "stageRelease").build()
        val saved = ReleaseArtifacts.verifyStaging(staging).mapValues { (path, _) -> staging.resolve("maven/$path").readBytes() }
        val metadata = staging.resolve("maven/dev/example/lib/maven-metadata.xml").readText()
        for (version in listOf("0.5", "1.0", "2.0")) assertTrue("<version>$version</version>" in metadata)
        for (name in listOf("first", "second")) {
            root.resolve("$name/build").deleteRecursively()
            root.resolve("$name/build.gradle.kts").appendText(
                "\ntasks.withType<Jar>().configureEach { doFirst { error(\"Must not rebuild\") } }\n",
            )
        }
        val graph = runner.withArguments(common + listOf("uploadStagedMavenToR2", "--dry-run")).build()
        assertFalse(graph.output.contains(":first:jar ") || graph.output.contains(":second:jar "))
        val result = runner.withArguments(common + "consumeStaging").build()
        assertFalse(result.tasks.any { it.path.endsWith(":jar") || it.path.endsWith(":sourcesJar") || "compile" in it.path })
        for ((path, expected) in saved) assertArrayEquals(expected, root.resolve("published/$path").readBytes())
    }
}
