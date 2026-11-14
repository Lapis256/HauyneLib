plugins {
    id("hauyne.kotlin-library")
    id("hauyne.maven-publish")
}

tasks.jar {
    manifest.attributes("Automatic-Module-Name" to "hauyne.config.core", "FMLModType" to "GAMELIBRARY")
}
