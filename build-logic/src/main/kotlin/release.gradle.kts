import dev.lapis256.hauyne.buildlogic.release.prepare.PrepareRelease
import dev.lapis256.hauyne.buildlogic.release.r2.DownloadMavenMetadata
import dev.lapis256.hauyne.buildlogic.release.r2.R2Task
import dev.lapis256.hauyne.buildlogic.release.r2.UploadStagedMavenToR2
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

val staging = layout.buildDirectory.dir("staging")

tasks.withType<R2Task>().configureEach {
    stagingDirectory = staging
    bucket = providers.gradleProperty("r2Bucket")
    endpoint = providers.environmentVariable("R2_ENDPOINT")
    accessKey = providers.environmentVariable("R2_ACCESS_KEY")
    secretKey = providers.environmentVariable("R2_SECRET_KEY")
}

tasks.register<PrepareRelease>("prepareRelease") {
    group = "publishing"
    description = "Update the release version and generate both changelogs with git-cliff"
    rootDirectory = layout.projectDirectory
    bump = providers.gradleProperty("releaseBump").orElse("auto")
}

tasks.register<DownloadMavenMetadata>("downloadMavenMetadata") {
    group = "publishing"
    coordinates.convention(emptyList())
}

tasks.register("stageRelease") {
    group = "publishing"
    description = "Build and check the release artifacts in the Maven staging repository"
    dependsOn(gradle.includedBuilds.filter { it.name == "build-logic" }.map { it.task(":test") })
}

tasks.register<UploadStagedMavenToR2>("uploadStagedMavenToR2") {
    group = "publishing"
}

gradle.projectsEvaluated {
    val publications = allprojects.flatMap { target ->
        target.tasks.withType(PublishToMavenRepository::class.java).filter { it.repository.name == "Staging" }
    }
    for (task in publications) {
        require(!task.publication.version.endsWith("-SNAPSHOT")) { "SNAPSHOT releases are not supported" }
    }
    for (module in publications.groupBy { "${it.publication.groupId}:${it.publication.artifactId}" }.values) {
        val ordered = module.sortedBy { it.publication.version }
        ordered.zipWithNext().forEach { (previous, next) -> next.mustRunAfter(previous) }
    }
    tasks.named("stageRelease") {
        dependsOn(allprojects.map { it.tasks.matching { task -> task.name == "check" } })
    }
}
