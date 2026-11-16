import dev.lapis256.hauyne.buildlogic.release.r2.DownloadMavenMetadata
import dev.lapis256.hauyne.buildlogic.release.staging.RecordReleaseArtifacts
plugins {
    `maven-publish`
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}

gradle.projectsEvaluated {
    val publication = publishing.publications.getByName("maven") as MavenPublication
    val coordinate = "${publication.groupId.replace('.', '/')}/${publication.artifactId}/${publication.version}"
    val base = "${publication.artifactId}-${publication.version}"
    val files = publication.artifacts.associate {
        val classifier = it.classifier?.takeIf(String::isNotBlank)?.let { value -> "-$value" }.orEmpty()
        it.file.relativeTo(rootDir).invariantSeparatorsPath to "$coordinate/$base$classifier.${it.extension}"
    } + mapOf(
        layout.buildDirectory.file("publications/maven/pom-default.xml").get().asFile.relativeTo(rootDir).invariantSeparatorsPath to
            "$coordinate/$base.pom",
        layout.buildDirectory.file("publications/maven/module.json").get().asFile.relativeTo(rootDir).invariantSeparatorsPath to
            "$coordinate/$base.module",
    )
    rootProject.tasks.named<DownloadMavenMetadata>("downloadMavenMetadata") {
        coordinates.add("${publication.groupId.replace('.', '/')}/${publication.artifactId}")
    }
    rootProject.tasks.named<RecordReleaseArtifacts>("stageRelease") {
        dependsOn(tasks.named("publishMavenPublicationToStagingRepository"))
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
            name = "Staging"
            url = rootProject.layout.buildDirectory.dir("staging/maven").get().asFile.toURI()
        }
    }
}

tasks.withType<PublishToMavenRepository>().matching { it.repository.name == "Staging" }.configureEach {
    dependsOn(rootProject.tasks.named("downloadMavenMetadata"))
    val manifest = rootProject.layout.buildDirectory.file("staging/SHA256SUMS")
    doFirst {
        require(!manifest.get().asFile.exists()) { "Cannot modify sealed staging" }
    }
}
