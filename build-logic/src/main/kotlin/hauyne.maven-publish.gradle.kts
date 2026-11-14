plugins {
    id("hauyne.library")
    `maven-publish`
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
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
    doFirst {
        require(!System.getenv("R2_ACCESS_KEY").isNullOrBlank() && !System.getenv("R2_SECRET_KEY").isNullOrBlank()) {
            "R2_ACCESS_KEY and R2_SECRET_KEY are required"
        }

        require(!System.getProperty("org.gradle.s3.endpoint").isNullOrBlank()) { "org.gradle.s3.endpoint is required" }
    }
}
