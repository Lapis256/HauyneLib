import dev.lapis256.hauyne.buildlogic.release.RecordReleaseArtifacts
import dev.lapis256.hauyne.buildlogic.release.ReleaseArtifacts
plugins {
    id("me.modmuss50.mod-publish-plugin")
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
