extensions.add("hauyneTargets", HauyneTargets(providers.fileContents(layout.projectDirectory.file("versions/targets.properties")).asText.get()))

tasks.register("verifyRequired") {
    group = "verification"
    description = "Build, test, publish locally and dry-run all registered release targets"
}

tasks.register("ciCheck") {
    group = "verification"
    description = "Run CI checks for all supported targets"
    dependsOn("spotlessCheck", "verifyRequired")
}

val validateRelease = tasks.register<ValidateReleaseSettings>("validateRelease") {
    group = "publishing"
    description = "Validate all publication settings before uploading"
    required = providers.environmentVariable("CI").map(String::toBooleanStrict).orElse(false)
    for (key in listOf("R2_ACCESS_KEY", "R2_SECRET_KEY", "R2_ENDPOINT", "CURSEFORGE_TOKEN", "MODRINTH_TOKEN")) {
        configurationPresent.put(key, providers.environmentVariable(key).map { it.isNotBlank() }.orElse(false))
    }
    for (key in listOf("curseforgeProjectId", "modrinthProjectId")) {
        configurationPresent.put(key, providers.gradleProperty(key).map { it.isNotBlank() }.orElse(false))
    }
}

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publish the verified release targets to Maven, CurseForge and Modrinth"
    dependsOn(validateRelease)
}

tasks.register<PrepareRelease>("prepareRelease") {
    group = "publishing"
    description = "Update the release version and generate both changelogs with git-cliff"
    rootDirectory = layout.projectDirectory
    bump = providers.gradleProperty("releaseBump").orElse("auto")
}

val localMaven = repositories.mavenLocal().url

tasks.register<RecordReleaseArtifacts>("recordReleaseArtifacts") {
    group = "publishing"
    description = "Compare and record the exact locally verified publication files"
    rootDirectory = layout.projectDirectory
    localRepository = file(localMaven)
    publicationFiles.convention(emptyMap())
    dependsOn("verifyRequired")
}
