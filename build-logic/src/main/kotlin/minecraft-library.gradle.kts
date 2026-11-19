plugins {
    id("library")
    id("minecraft-artifact")
    id("maven-publishing")
}

tasks.withType<Jar>().configureEach {
    manifest.attributes("FMLModType" to "GAMELIBRARY")
}
