import dev.lapis256.hauyne.buildlogic.release.prepare.PrepareRelease
import dev.lapis256.hauyne.buildlogic.release.r2.DownloadMavenMetadata
import dev.lapis256.hauyne.buildlogic.release.r2.R2Task
import dev.lapis256.hauyne.buildlogic.release.r2.UploadStagedMavenToR2
import dev.lapis256.hauyne.buildlogic.release.r2.VerifyPublishedMaven
import dev.lapis256.hauyne.buildlogic.release.staging.RecordReleaseArtifacts
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

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

tasks.register<RecordReleaseArtifacts>("stageRelease") {
    group = "publishing"
    description = "Build, check and seal the Maven staging repository"
    rootDirectory = layout.projectDirectory
    stagingDirectory = staging
    publicationFiles.convention(emptyMap())
    dependsOn(gradle.includedBuilds.filter { it.name == "build-logic" }.map { it.task(":test") })
}

tasks.register<UploadStagedMavenToR2>("uploadStagedMavenToR2") {
    group = "publishing"
}

tasks.register<VerifyPublishedMaven>("verifyPublishedMaven") {
    group = "publishing"
}

gradle.projectsEvaluated {
    val publications = allprojects.flatMap { target ->
        target.extensions.findByType(PublishingExtension::class.java)?.publications
            ?.withType(MavenPublication::class.java)?.map { publication ->
                require(!publication.version.endsWith("-SNAPSHOT")) { "SNAPSHOT releases are not supported" }
                Triple(
                    "${publication.groupId}:${publication.artifactId}",
                    publication.version,
                    target.tasks.named("publish${publication.name.replaceFirstChar(Char::uppercaseChar)}PublicationToStagingRepository"),
                )
            }.orEmpty()
    }
    publications.groupBy { it.first }.values.forEach { module ->
        module.sortedBy { it.second }.zipWithNext().forEach { (previous, next) -> next.third.configure { mustRunAfter(previous.third) } }
    }
    tasks.named("stageRelease") {
        dependsOn(allprojects.map { it.tasks.matching { task -> task.name == "check" } })
    }
}
