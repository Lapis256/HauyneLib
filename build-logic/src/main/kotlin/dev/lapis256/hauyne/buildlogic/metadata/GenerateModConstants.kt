package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateModConstants : DefaultTask() {
    @get:Input
    abstract val modId: Property<String>

    @get:Input
    abstract val modPackage: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val file = outputDirectory.file("${modPackage.get().replace('.', '/')}/HauyneConstants.java").get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package ${modPackage.get()};

            public final class HauyneConstants {
                public static final String MOD_ID = "${modId.get()}";
            }

            """.trimIndent(),
            Charsets.UTF_8,
        )
    }
}
