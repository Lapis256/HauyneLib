# build-logic/src/main/kotlin/hauyne.mod-publish.gradle.kts

- modFile · variable · L38-L38 — val modFile = publishMods.file
- validateModRelease · variable · L43-L62 — val validateModRelease = tasks.register<ValidateReleaseSettings>("validateModRelease") { required = providers.gradleProperty("modPublishDryRun").map { !it.toBooleanStrict() }.orElse(false) for (key in listOf("curseforgeProjectId", "modrinthProjectId")) { configurationPresent.put( key, providers.gradleProperty(key).map { it.isNotBlank() }.orElse(false), ) } for (key in listOf("CURSEFORGE_TOKEN", "MODRINTH_TOKEN")) { configurationPresent.put( key, providers.environmentVariable(key).map { it.isNotBlank() }.orElse(false), ) } }
- releaseManifest · variable · L70-L70 — val releaseManifest = providers.gradleProperty("releaseManifest")
- releaseRoot · variable · L71-L71 — val releaseRoot = rootProject.layout.projectDirectory.asFile
- releaseFile · variable · L72-L72 — val releaseFile = publishMods.file
