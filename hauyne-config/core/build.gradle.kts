plugins {
    id("library")
    id("maven-publishing")
}

tasks.jar {
    manifest.attributes("Automatic-Module-Name" to "hauyne.config.core")
}
