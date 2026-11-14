import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension

plugins {
    id("hauyne.minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.legacyforge)
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

extensions.configure<LegacyForgeExtension> {
    enable {
        forgeVersion = hauyneMinecraft.versionProperty("forgeVersion").get()
    }

    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    api(project(":hauyne-config:minecraft:$mc"))
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
