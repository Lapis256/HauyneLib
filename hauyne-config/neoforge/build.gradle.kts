import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("hauyne.minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

val mc = stonecutter.current.version

extensions.configure<NeoForgeExtension> {
    enable {
        version = hauyneMinecraft.versionProperty("neoForgeVersion").get()
    }

    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    api(project(":hauyne-config:minecraft:${stonecutter.current.project}"))
}
