plugins {
    id("me.modmuss50.mod-publish-plugin")
}

publishMods {
    version = providers.provider { "${project.version}-${modLoaders.get().single()}" }
    displayName = file.map {
        it.asFile.nameWithoutExtension
    }

    changelog = "${rootProject.property("modName")} ${rootProject.property("modVersion")}"
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

val validateModRelease = tasks.register<ValidateModRelease>("validateModRelease") {
    dryRun = providers.gradleProperty("modPublishDryRun").map(String::toBooleanStrict).orElse(true)
    for (key in listOf("curseforgeProjectId", "modrinthProjectId")) {
        configurationPresent.put(
            key,
            providers.gradleProperty(key).map {
                it.isNotBlank()
            }.orElse(false),
        )
    }

    for (key in listOf("CURSEFORGE_TOKEN", "MODRINTH_TOKEN")) {
        configurationPresent.put(
            key,
            providers.environmentVariable(key).map {
                it.isNotBlank()
            }.orElse(false),
        )
    }
}

tasks.matching {
    it.name.startsWith("publish") &&
        (it.name.contains("Curseforge", true) || it.name.contains("Modrinth", true) || it.name == "publishMods")
}.configureEach {
    dependsOn(validateModRelease)
}
