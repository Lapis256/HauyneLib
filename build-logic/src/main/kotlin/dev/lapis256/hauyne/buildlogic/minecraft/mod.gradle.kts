import dev.lapis256.hauyne.buildlogic.metadata.GenerateFmlModMetadata
import dev.lapis256.hauyne.buildlogic.metadata.GenerateModConstants
import dev.lapis256.hauyne.buildlogic.metadata.HauyneFmlMetadataExtension
import dev.lapis256.hauyne.buildlogic.metadata.HauyneModMetadataExtension
import dev.lapis256.hauyne.buildlogic.release.RecordReleaseArtifacts
import dev.lapis256.hauyne.buildlogic.release.ReleaseArtifacts

plugins {
    id("minecraft-library")
    id("me.modmuss50.mod-publish-plugin")
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

val configBackend = project(":hauyne-config:${parent!!.name}:${project.name}")
val configMinecraft = project(":hauyne-config:minecraft:${project.name}")

dependencies {
    implementation(configBackend)
}

for (loaderPlugin in listOf("net.neoforged.moddev", "net.neoforged.moddev.legacyforge")) {
    pluginManager.withPlugin(loaderPlugin) {
        dependencies {
            add("jarJar", project(":hauyne-config:core"))
            add("jarJar", configMinecraft)
            add("jarJar", configBackend)
        }
    }
}

publishMods {
    version = providers.provider { "${project.version}-${modLoaders.get().single()}" }
    displayName = file.map {
        it.asFile.nameWithoutExtension
    }

    changelog = providers.gradleProperty("releaseChangelog").flatMap {
        providers.fileContents(rootProject.layout.projectDirectory.file(it)).asText
    }.orElse("No changelog provided.")
    type = me.modmuss50.mpp.ReleaseType.valueOf(rootProject.property("releaseChannel").toString())
    dryRun = providers.gradleProperty("modPublishDryRun").map(String::toBooleanStrict).orElse(true)
    curseforge {
        projectId = providers.gradleProperty("curseforgeProjectId")
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").orElse("")
        client = true
        server = true
    }

    modrinth {
        projectId = providers.gradleProperty("modrinthProjectId")
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse("")
        environment = me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment.CLIENT_AND_SERVER
    }
}

rootProject.tasks.named("recordReleaseArtifacts") {
    dependsOn(tasks.named("publishMods"))
}

rootProject.tasks.named("publishRelease") {
    dependsOn(tasks.named("publishMods"))
}

val modFile = publishMods.file
rootProject.tasks.named<RecordReleaseArtifacts>("recordReleaseArtifacts") {
    modFiles.from(modFile)
}

tasks.matching {
    it.name.startsWith("publish") &&
        (it.name.contains("Curseforge", true) || it.name.contains("Modrinth", true) || it.name == "publishMods")
}.configureEach {
    val releaseManifest = providers.gradleProperty("releaseManifest")
    val releaseRoot = rootProject.layout.projectDirectory.asFile
    val releaseFile = publishMods.file
    doFirst {
        releaseManifest.orNull?.let {
            ReleaseArtifacts.verify(releaseRoot, releaseRoot.resolve(it), listOf(releaseFile.get().asFile))
        }
    }
}
