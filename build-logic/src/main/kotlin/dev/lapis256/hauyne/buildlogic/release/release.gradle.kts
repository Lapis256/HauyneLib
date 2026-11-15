import dev.lapis256.hauyne.buildlogic.release.PrepareRelease
import dev.lapis256.hauyne.buildlogic.release.RecordReleaseArtifacts

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publish the verified release targets to Maven, CurseForge and Modrinth"
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
}

val publishedJvmModules = listOf(":hauyne-config:core", ":hauyne-config:ksp", ":hauyne-config:nightconfig")

val prepareJvmPublications = tasks.register("prepareJvmPublications") {
    dependsOn(publishedJvmModules.map { "$it:publishToMavenLocal" })
}
tasks.register("publishJvm") {
    dependsOn(publishedJvmModules.map { "$it:publish" })
}
