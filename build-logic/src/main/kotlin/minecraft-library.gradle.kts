import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.lapis256.hauyne.buildlogic.minecraft.HauyneMinecraftExtension

plugins {
    id("library")
    id("maven-publishing")
}

val minecraft = extensions.create<HauyneMinecraftExtension>("hauyneMinecraft")
minecraft.versionPropertiesFile.convention(layout.projectDirectory.file("gradle.properties"))

base {
    archivesName = parent!!.path.trim(':').replace(':', '-')
}

val sc = extensions.getByType<StonecutterBuildExtension>()

java {
    val javaVersion = when {
        sc.current.parsed >= "26.1" -> 25
        sc.current.parsed >= "1.20.5" -> 21
        sc.current.parsed >= "1.18" -> 17
        sc.current.parsed >= "1.17" -> 16
        else -> 8
    }
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

afterEvaluate {
    version = "${project.name}-${rootProject.version}"
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
