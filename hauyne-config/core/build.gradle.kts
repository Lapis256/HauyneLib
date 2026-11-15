plugins {
    id("hauyne.library")
    id("hauyne.maven-publish")
}

tasks.jar {
    manifest.attributes("Automatic-Module-Name" to "hauyne.config.core")
}
