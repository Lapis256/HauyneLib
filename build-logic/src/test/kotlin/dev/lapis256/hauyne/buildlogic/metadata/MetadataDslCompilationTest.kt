package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties

class MetadataDslCompilationTest {
    @TempDir
    lateinit var root: File

    @Test
    fun loaderSpecificOperationsAreCheckedByTheKotlinCompiler() {
        val classpath = Properties().apply {
            MetadataDslCompilationTest::class.java.classLoader.getResourceAsStream("plugin-under-test-metadata.properties")!!.use { load(it) }
        }.getProperty("implementation-classpath").split(File.pathSeparator)
            .joinToString(", ") { "\"${File(it).invariantSeparatorsPath}\"" }
        root.resolve("settings.gradle.kts").writeText("rootProject.name = \"metadata-dsl-compilation\"\n")
        fun script(body: String) {
            root.resolve("build.gradle.kts").writeText(
                """
                import dev.lapis256.hauyne.buildlogic.metadata.HauyneModMetadataExtension

                buildscript { dependencies { classpath(files($classpath)) } }

                val metadata = objects.newInstance(HauyneModMetadataExtension::class.java)
                $body
                """.trimIndent(),
            )
        }
        val runner = GradleRunner.create().withProjectDir(root).withArguments("help", "--offline", "--console=plain")
        script(
            """
            metadata.forge { dependencies { require("api"); optional("integration") } }
            val neo = objects.newInstance(HauyneModMetadataExtension::class.java)
            neo.neoforge { dependencies { incompatible("broken_mod"); discouraged("slow_mod") } }
            """.trimIndent(),
        )
        runner.build()
        script(
            """
            metadata.forge {
                dependencies {
                    incompatible("broken_mod")
                    discouraged("slow_mod")
                    require("api") { type.set(type) }
                }
            }
            """.trimIndent(),
        )
        val output = runner.buildAndFail().output
        for (name in listOf("incompatible", "discouraged", "set")) {
            assertTrue(output.lineSequence().any { it.contains("Unresolved reference") && it.contains(name) }, output)
        }
    }
}
