import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("hauyne.minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
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

extensions.configure<NeoForgeExtension> {
    enable {
        version = hauyneMinecraft.versionProperty("neoForgeVersion").get()
    }

    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    api(project(":hauyne-config:minecraft:$mc"))
}
