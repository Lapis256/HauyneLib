plugins {
    id("hauyne.minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

neoForge {
    version = hauyneMinecraft.versionProperty("neoForgeVersion").get()

    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    api(project(":hauyne-config:minecraft:${hauyneMinecraft.configMinecraftTarget.get()}"))
}
