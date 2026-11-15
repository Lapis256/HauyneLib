import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension

plugins {
    id("hauyne.minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.legacyforge)
}

hauyneMinecraft.releaseEnabled = false

val mc = stonecutter.current.version

extensions.configure<LegacyForgeExtension> {
    enable {
        forgeVersion = hauyneMinecraft.versionProperty("forgeVersion").get()
    }

    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    api(project(":hauyne-config:minecraft:${hauyneMinecraft.versionProperty("configMinecraftTarget").get()}"))
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
