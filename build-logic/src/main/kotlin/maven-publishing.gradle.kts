import dev.lapis256.hauyne.buildlogic.release.RecordReleaseArtifacts
import dev.lapis256.hauyne.buildlogic.release.ReleaseArtifacts
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
plugins {
    `maven-publish`
}

val releaseManifest = providers.gradleProperty("releaseManifest")

publishing.publications.create<MavenPublication>("maven") {
    if (!releaseManifest.isPresent) from(components["java"])
}

rootProject.tasks.named("recordReleaseArtifacts") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

rootProject.tasks.named("publishRelease") {
    if (releaseManifest.isPresent) dependsOn(tasks.named("publish"))
}

gradle.projectsEvaluated {
    val publication = publishing.publications.getByName("maven") as MavenPublication
    val coordinate = "${publication.groupId.replace('.', '/')}/${publication.artifactId}/${publication.version}"
    val base = "${publication.artifactId}-${publication.version}"
    if (releaseManifest.isPresent) {
        val assets = rootProject.layout.projectDirectory.dir(".release/assets")
        publication.artifact(assets.file("$base.jar"))
        publication.artifact(assets.file("$base-sources.jar")) { classifier = "sources" }
        publication.artifact(assets.file("$base.module")) { extension = "module" }
        tasks.withType<GenerateMavenPom>().configureEach {
            destination = assets.file("$base.pom").asFile
            enabled = false
        }
        return@projectsEvaluated
    }
    val files = publication.artifacts.associate {
        val classifier = it.classifier?.takeIf(String::isNotBlank)?.let { value -> "-$value" }.orEmpty()
        it.file.relativeTo(rootDir).invariantSeparatorsPath to "$coordinate/$base$classifier.${it.extension}"
    } + mapOf(
        layout.buildDirectory.file("publications/maven/pom-default.xml").get().asFile.relativeTo(rootDir).invariantSeparatorsPath to
            "$coordinate/$base.pom",
        layout.buildDirectory.file("publications/maven/module.json").get().asFile.relativeTo(rootDir).invariantSeparatorsPath to
            "$coordinate/$base.module",
    )
    rootProject.tasks.named<RecordReleaseArtifacts>("recordReleaseArtifacts") {
        publicationFiles.putAll(files)
    }
}

afterEvaluate {
    publishing.publications.withType<MavenPublication>().configureEach {
        artifactId = project.extensions.getByType<BasePluginExtension>().archivesName.get()
    }
}

configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        groupId = rootProject.property("mavenGroup").toString()
        pom {
            name = providers.provider {
                project.extensions.getByType<BasePluginExtension>().archivesName.get()
            }

            description = "HauyneLib configuration library"
            url = rootProject.property("projectUrl").toString()
            licenses {
                license {
                    name = rootProject.property("license").toString()
                    url = "https://opensource.org/license/mit"
                }
            }

            developers {
                developer {
                    id = "Lapis256"
                    name = "Lapis256"
                }
            }

            scm {
                url = rootProject.property("projectUrl").toString()
                connection = "scm:git:${rootProject.property("projectUrl")}.git"
            }
        }
    }

    repositories {
        maven {
            name = "R2"
            url = uri("s3://" + rootProject.property("r2Bucket"))
            credentials(AwsCredentials::class) {
                accessKey = providers.environmentVariable("R2_ACCESS_KEY").orNull ?: ""
                secretKey = providers.environmentVariable("R2_SECRET_KEY").orNull ?: ""
            }
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    val releaseRoot = rootProject.layout.projectDirectory.asFile
    doFirst {
        releaseManifest.orNull?.let {
            ReleaseArtifacts.verify(
                releaseRoot,
                releaseRoot.resolve(it),
                publication.artifacts.map { artifact -> artifact.file } +
                    releaseRoot.resolve(".release/assets/${publication.artifactId}-${publication.version}.pom"),
            )
        }

        require(!System.getProperty("org.gradle.s3.endpoint").isNullOrBlank()) { "org.gradle.s3.endpoint is required" }
    }
}
