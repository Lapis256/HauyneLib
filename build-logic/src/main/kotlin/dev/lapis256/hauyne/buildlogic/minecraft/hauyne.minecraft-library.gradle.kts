import dev.lapis256.hauyne.buildlogic.library.HauyneLibraryExtension
import dev.lapis256.hauyne.buildlogic.minecraft.HauyneMinecraftExtension
plugins {
    id("hauyne.library")
    id("hauyne.maven-publish")
}

val minecraft = extensions.create<HauyneMinecraftExtension>("hauyneMinecraft")
minecraft.releaseEnabled.convention(true)
minecraft.versionPropertiesFile.convention(layout.projectDirectory.file("gradle.properties"))
minecraft.minecraftVersion.convention(minecraft.versionProperty("minecraftVersion"))
extensions.getByType<HauyneLibraryExtension>().componentPath.convention(parent!!.path)

java {
    toolchain.languageVersion = minecraft.versionProperty("javaVersion").map { JavaLanguageVersion.of(it.toInt()) }
}

afterEvaluate {
    version = "${project.name.substringBefore('-')}-${rootProject.version}"
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
