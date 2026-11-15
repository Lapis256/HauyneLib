plugins {
    id("hauyne.minecraft-library")
}

val metadata = extensions.create<HauyneModMetadataExtension>("hauyneModMetadata")
metadata.description.convention("HauyneLib configuration library")
val constants = tasks.register<GenerateModConstants>("generateModConstants") {
    modId = metadata.modId
    modPackage = metadata.modPackage
    outputDirectory = layout.buildDirectory.dir("generated/sources/modConstants")
}

sourceSets.main {
    java.srcDir(constants)
}
