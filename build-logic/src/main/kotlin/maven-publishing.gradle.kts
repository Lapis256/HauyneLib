import dev.lapis256.hauyne.buildlogic.release.r2.DownloadMavenMetadata
plugins {
    `maven-publish`
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}

gradle.projectsEvaluated {
    val publication = publishing.publications.getByName("maven") as MavenPublication
    rootProject.tasks.named<DownloadMavenMetadata>("downloadMavenMetadata") {
        coordinates.add("${publication.groupId.replace('.', '/')}/${publication.artifactId}")
    }
    rootProject.tasks.named("stageRelease") {
        dependsOn(tasks.named("publishMavenPublicationToStagingRepository"))
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
}
