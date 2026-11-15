pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "HauyneLib"

includeBuild("build-logic")

include(
    ":examples:jvm",
    ":hauyne-config:core",
    ":hauyne-config:forge",
    ":hauyne-config:ksp",
    ":hauyne-config:memory",
    ":hauyne-config:minecraft",
    ":hauyne-config:neoforge",
    ":hauyne-config:nightconfig",
    ":hauyne-lib:forge",
    ":hauyne-lib:neoforge",
)

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"
    create(":hauyne-config:minecraft") {
        version("26.1", "26.1.2")
        version("21.1", "1.21.1")
        version("20.1", "1.20.1").apply {
            buildscript = "build.legacy.gradle.kts"
        }
        vcsVersion = "26.1"
    }
    create(":hauyne-config:neoforge") {
        version("26.1", "26.1.2")
        version("21.1", "1.21.1")
        vcsVersion = "26.1"
    }
    create(":hauyne-lib:neoforge") {
        version("26.1", "26.1.2")
        version("21.1", "1.21.1")
        vcsVersion = "26.1"
    }
    create(":hauyne-config:forge") {
        version("20.1", "1.20.1")
        vcsVersion = "20.1"
    }
    create(":hauyne-lib:forge") {
        version("20.1", "1.20.1")
        vcsVersion = "20.1"
    }
}
