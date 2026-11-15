import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.lapis256.hauyne.buildlogic.release.PrepareRelease
import dev.lapis256.hauyne.buildlogic.release.RecordReleaseArtifacts

tasks.register("ciCheck") {
    group = "verification"
    description = "Run CI checks for all supported targets"
    dependsOn("spotlessCheck", "verifyJvm")
}

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

val jvmModules = listOf(":hauyne-config:core", ":hauyne-config:ksp", ":hauyne-config:memory", ":hauyne-config:nightconfig", ":examples:jvm")
val publishedJvmModules = listOf(":hauyne-config:core", ":hauyne-config:ksp", ":hauyne-config:nightconfig")

val verifyJvm = tasks.register("verifyJvm") {
    dependsOn(jvmModules.map { "$it:check" })
}
val prepareJvmPublications = tasks.register("prepareJvmPublications") {
    dependsOn(publishedJvmModules.map { "$it:publishToMavenLocal" })
}
tasks.register("publishJvm") {
    dependsOn(publishedJvmModules.map { "$it:publish" })
}
gradle.projectsEvaluated {
    for (controller in allprojects.filter { it.extensions.findByType<StonecutterControllerExtension>() != null }) {
        for (target in controller.subprojects) {
            val verify = tasks.register("verify${target.path.replace(':', '_')}") {
                dependsOn("${target.path}:check")
            }
            tasks.named("ciCheck") { dependsOn(verify) }
        }
    }
}
