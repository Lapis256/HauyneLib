plugins {
    id("minecraft-config-backend")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

neoForge {
    version = hauyneMinecraft.versionProperty("neoForgeVersion").get()

    addModdingDependenciesTo(sourceSets.test.get())
}
