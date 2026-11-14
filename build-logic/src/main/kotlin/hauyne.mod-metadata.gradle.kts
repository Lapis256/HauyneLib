plugins {
    id("hauyne.minecraft-library")
}

val metadata = extensions.create<HauyneModMetadataExtension>("hauyneModMetadata")
val constants = tasks.register<GenerateModConstants>("generateModConstants") {
    modId = metadata.modId
    modPackage = metadata.modPackage
    outputDirectory = layout.buildDirectory.dir("generated/sources/modConstants")
}

sourceSets.main {
    java.srcDir(constants)
}

tasks.withType<Jar>().configureEach {
    manifest.attributes("FMLModType" to "MOD")
}

afterEvaluate {
    val loader = metadata.loader.get()
    val dependencies = listOf(
        "minecraft" to metadata.minecraftVersionRange.get(),
        (if (loader == HauyneModLoader.FORGE) "forge" else "neoforge") to metadata.loaderVersionRange.get(),
    ).joinToString("\n") {
            (id, range) ->
        """
        [[dependencies.${metadata.modId.get()}]]
        modId="$id"
        versionRange="$range"
        ${if (loader == HauyneModLoader.FORGE) "mandatory=true" else "type=\"required\""}
        ordering="NONE"
        side="BOTH"
        """.trimIndent()
    }

    val values = mapOf(
        "modId" to metadata.modId.get(),
        "modName" to metadata.modName.get(),
        "version" to project.version.toString(),
        "dependencies" to dependencies,
        "loaderVersion" to metadata.javaFmlLoaderVersionRange.get(),
        "license" to metadata.license.get(),
    )
    tasks.named<ProcessResources>("processResources") {
        inputs.properties(values)
        filesMatching("META-INF/*.toml") {
            expand(values)
        }
    }
}
