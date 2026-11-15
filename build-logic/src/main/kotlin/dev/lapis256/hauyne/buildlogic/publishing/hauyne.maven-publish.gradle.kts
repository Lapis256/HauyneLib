import dev.lapis256.hauyne.buildlogic.minecraft.HauyneMinecraftExtension
import dev.lapis256.hauyne.buildlogic.release.RecordReleaseArtifacts
import dev.lapis256.hauyne.buildlogic.release.ReleaseArtifacts
plugins {
    `maven-publish`
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}

afterEvaluate {
    if (extensions.findByType<HauyneMinecraftExtension>()?.releaseEnabled?.get() != false) {
        rootProject.tasks.named("verifyRequired") {
            dependsOn(tasks.named("publishToMavenLocal"))
        }

        rootProject.tasks.named("publishRelease") {
            dependsOn(tasks.named("publish"))
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
            rootProject.tasks.named<RecordReleaseArtifacts>("recordReleaseArtifacts") {
                publicationFiles.putAll(files)
            }
        }
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
    mustRunAfter(":validateRelease")
    val releaseManifest = providers.gradleProperty("releaseManifest")
    val releaseRoot = rootProject.layout.projectDirectory.asFile
    doFirst {
        releaseManifest.orNull?.let {
            ReleaseArtifacts.verify(releaseRoot, releaseRoot.resolve(it), publication.artifacts.map { artifact -> artifact.file })
        }

        require(!System.getenv("R2_ACCESS_KEY").isNullOrBlank() && !System.getenv("R2_SECRET_KEY").isNullOrBlank()) {
            "R2_ACCESS_KEY and R2_SECRET_KEY are required"
        }

        require(!System.getProperty("org.gradle.s3.endpoint").isNullOrBlank()) { "org.gradle.s3.endpoint is required" }
    }
}
