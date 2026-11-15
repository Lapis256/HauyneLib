import org.ec4j.core.Resource
import org.ec4j.core.ResourcePropertiesService
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.diffplug.spotless")
}

repositories {
    mavenCentral()
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

spotless {
    encoding("UTF-8")
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    java {
        target("**/src/**/*.java")
        targetExclude("**/build/**", "**/.gradle/**")
        val editorConfig = ResourcePropertiesService.builder().build().queryProperties(
            Resource.Resources.ofPath(file("src/main/java/Formatting.java").toPath(), Charsets.UTF_8),
        ).properties
        eclipse(catalog.findVersion("eclipse").get().requiredVersion).configProperties(
            """
            org.eclipse.jdt.core.formatter.tabulation.char=${editorConfig.getValue("indent_style").sourceValue}
            org.eclipse.jdt.core.formatter.tabulation.size=${editorConfig.getValue("indent_size").sourceValue}
            org.eclipse.jdt.core.formatter.indentation.size=${editorConfig.getValue("indent_size").sourceValue}
            org.eclipse.jdt.core.formatter.lineSplit=${editorConfig.getValue("max_line_length").sourceValue}
            """.trimIndent(),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(catalog.findVersion("ktlint").get().requiredVersion).setEditorConfigPath(rootProject.file(".editorconfig"))
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(catalog.findVersion("ktlint").get().requiredVersion).setEditorConfigPath(rootProject.file(".editorconfig"))
    }
}
