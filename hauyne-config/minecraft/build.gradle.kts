import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension

plugins {
    id("hauyne.minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg) apply false
    alias(libs.plugins.legacyforge) apply false
}

val mc = stonecutter.current.version

hauyneLibrary {
    componentPath = parent!!.path
}

hauyneMinecraft {
    minecraftVersion = mc
    versionPropertiesFile = rootProject.layout.projectDirectory.file("versions/$mc/gradle.properties")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(hauyneMinecraft.versionProperty("javaVersion").get().toInt())
}

if (stonecutter.eval(mc, "<1.21")) {
    apply(plugin = libs.plugins.legacyforge.get().pluginId)
    extensions.configure<LegacyForgeExtension> {
        enable {
            mcpVersion = hauyneMinecraft.versionProperty("mcpVersion").get()
        }

        addModdingDependenciesTo(sourceSets.test.get())
    }
} else {
    apply(plugin = libs.plugins.mdg.get().pluginId)
    extensions.configure<NeoForgeExtension> {
        enable {
            neoFormVersion = hauyneMinecraft.versionProperty("neoFormVersion").get()
        }

        addModdingDependenciesTo(sourceSets.test.get())
    }
}

dependencies {
    api(project(":hauyne-config:core"))
}

configurations.matching {
    it.name == "reobfApiElements"
}.configureEach {
    extendsFrom(configurations.getByName("api"))
}

configurations.matching {
    it.name == "reobfRuntimeElements"
}.configureEach {
    extendsFrom(configurations.getByName("implementation"), configurations.getByName("runtimeOnly"))
}
