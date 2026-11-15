plugins {
    id("minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

neoForge {
    neoFormVersion = stonecutter.properties["neoFormVersion"]

    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    api(project(":hauyne-config:core"))
}
