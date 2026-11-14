plugins {
    id("hauyne.maven-publish")
}

val minecraft = extensions.create<HauyneMinecraftExtension>("hauyneMinecraft")

afterEvaluate {
    val series = minecraft.minecraftVersion.get().removePrefix("1.").split('.').take(2).joinToString(".")
    version = "$series-${rootProject.property("modVersion")}"
    val artifact = base.archivesName.get()
    // Gradle must distinguish equally named Stonecutter version nodes.
    group = "${rootProject.property("mavenGroup")}.${artifact.replace('-', '.')}"
}

tasks.withType<Jar>().configureEach {
    manifest.attributes(
        "Automatic-Module-Name" to base.archivesName.map {
            it.replace('-', '.')
        },
        "FMLModType" to "GAMELIBRARY",
    )
}
