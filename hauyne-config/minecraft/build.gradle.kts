plugins {
    id("hauyne.minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

neoForge {
    neoFormVersion = hauyneMinecraft.versionProperty("neoFormVersion").get()

    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    api(project(":hauyne-config:core"))
}
