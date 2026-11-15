plugins {
    id("minecraft-config-backend")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

neoForge {
    version = stonecutter.properties["neoForgeVersion"]

    addModdingDependenciesTo(sourceSets.test.get())
}
