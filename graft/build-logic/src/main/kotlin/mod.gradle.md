# build-logic/src/main/kotlin/mod.gradle.kts

- metadata · variable · L18-L18 — val metadata = extensions.create<HauyneModMetadataExtension>("hauyneMetadata")
- constants · variable · L27-L31 — val constants = tasks.register<GenerateModConstants>("generateModConstants") { modId = metadata.modId modPackage = metadata.modPackage outputDirectory = layout.buildDirectory.dir("generated/sources/modConstants/kotlin") }
- fml · variable · L37-L37 — val fml = metadata.selectedMetadata
- defaultMinecraftVersionRange · variable · L38-L38 — val defaultMinecraftVersionRange = extensions.getByType<StonecutterBuildExtension>().current.version.eq()
- generateMetadata · variable · L39-L54 — val generateMetadata = tasks.register<GenerateFmlModMetadata>("generateModMetadata") { modId = metadata.modId modName = metadata.modName modAuthor = metadata.modAuthor issueTrackerUrl = metadata.issueTrackerUrl displayUrl = metadata.displayUrl license = metadata.license modDescription = metadata.description loader = metadata.selectedLoader minecraftVersionRange = fml.flatMap { it.minecraftVersionRange } .orElse(defaultMinecraftVersionRange) .map { it.versionRange } loaderVersionRange = fml.flatMap { it.loaderVersionRange }.map { it.versionRange } javaFmlLoaderVersionRange = fml.flatMap { it.javaFmlLoaderVersionRange }.map { it.versionRange } modDependencies = fml.flatMap { it.dependencyEntries } }
- loader · variable · L57-L57 — val loader = requireNotNull(metadata.selectedLoader.orNull) { "Configure hauyneMetadata.forge {} or hauyneMetadata.neoforge {}" }
- configBackend · variable · L73-L73 — val configBackend = project(":hauyne-config:${parent!!.name}:${project.name}")
- configMinecraft · variable · L74-L74 — val configMinecraft = project(":hauyne-config:minecraft:${project.name}")
- modFile · variable · L144-L144 — val modFile = publishMods.file
- releaseManifest · variable · L153-L153 — val releaseManifest = providers.gradleProperty("releaseManifest")
- releaseRoot · variable · L154-L154 — val releaseRoot = rootProject.layout.projectDirectory.asFile
- releaseFile · variable · L155-L155 — val releaseFile = publishMods.file
