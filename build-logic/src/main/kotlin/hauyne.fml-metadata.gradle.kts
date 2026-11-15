plugins {
    id("hauyne.mod-metadata")
}

val metadata = extensions.getByType<HauyneModMetadataExtension>()
val fml = extensions.create<HauyneFmlMetadataExtension>("hauyneFmlMetadata")
val generateMetadata = tasks.register<GenerateFmlModMetadata>("generateModMetadata") {
    modId = metadata.modId
    modName = metadata.modName
    license = metadata.license
    modDescription = metadata.description
    loader = fml.loader
    minecraftVersionRange = fml.minecraftVersionRange
    loaderVersionRange = fml.loaderVersionRange
    javaFmlLoaderVersionRange = fml.javaFmlLoaderVersionRange
}

afterEvaluate {
    val loader = fml.loader.get()
    generateMetadata.configure {
        modVersion = project.version.toString()
        outputFile = layout.buildDirectory.file("generated/resources/modMetadata/${loader.metadataFileName}")
    }
}

tasks.named<ProcessResources>("processResources") {
    from(generateMetadata) { into("META-INF") }
}

tasks.withType<Jar>().configureEach {
    manifest.attributes("FMLModType" to "MOD")
}
