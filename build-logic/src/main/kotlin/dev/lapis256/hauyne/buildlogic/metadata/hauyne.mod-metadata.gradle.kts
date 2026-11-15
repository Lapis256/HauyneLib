import dev.lapis256.hauyne.buildlogic.metadata.GenerateModConstants
import dev.lapis256.hauyne.buildlogic.metadata.HauyneModMetadataExtension
plugins {
    id("hauyne.minecraft-library")
}

val metadata = extensions.create<HauyneModMetadataExtension>("hauyneModMetadata")
metadata.modId.convention(providers.gradleProperty("modId"))
metadata.modName.convention(providers.gradleProperty("modName"))
metadata.modPackage.convention(providers.gradleProperty("modPackage"))
metadata.license.convention(providers.gradleProperty("license"))
metadata.description.convention(providers.gradleProperty("modDescription"))
val constants = tasks.register<GenerateModConstants>("generateModConstants") {
    modId = metadata.modId
    modPackage = metadata.modPackage
    outputDirectory = layout.buildDirectory.dir("generated/sources/modConstants/kotlin")
}

kotlin.sourceSets.main {
    kotlin.srcDir(constants)
}
