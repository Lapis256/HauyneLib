plugins {
    id("hauyne.library")
    id("hauyne.maven-publish")
}

val minecraft = extensions.create<HauyneMinecraftExtension>("hauyneMinecraft")
val targets = HauyneTargets(providers.fileContents(rootProject.layout.projectDirectory.file("versions/targets.properties")).asText.get())
val target = targets.all.single { it.series == project.name }
minecraft.minecraftVersion.convention(target.minecraft)
minecraft.versionPropertiesFile.convention(rootProject.layout.projectDirectory.file("versions/${target.series}/gradle.properties"))
extensions.getByType<HauyneLibraryExtension>().componentPath.convention(parent!!.path)

java {
    toolchain.languageVersion = minecraft.versionProperty("javaVersion").map { JavaLanguageVersion.of(it.toInt()) }
}

afterEvaluate {
    version = "${target.series}-${rootProject.version}"
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
