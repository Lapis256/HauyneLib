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
    ":hauyne-config:core",
    ":hauyne-config:ksp",
    ":hauyne-config:memory",
    ":hauyne-config:nightconfig",
    ":examples:jvm",
)

include(
    ":hauyne-config:minecraft",
    ":hauyne-config:neoforge",
    ":hauyne-lib:neoforge",
    ":hauyne-config:forge",
    ":hauyne-lib:forge",
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
        version("26.1-neoforge", "26.1.2")
        version("21.1-neoforge", "1.21.1")
        vcsVersion = "26.1-neoforge"
    }
    create(":hauyne-lib:neoforge") {
        version("26.1-neoforge", "26.1.2")
        version("21.1-neoforge", "1.21.1")
        vcsVersion = "26.1-neoforge"
    }
    create(":hauyne-config:forge") {
        version("20.1-forge", "1.20.1")
        vcsVersion = "20.1-forge"
    }
    create(":hauyne-lib:forge") {
        version("20.1-forge", "1.20.1")
        vcsVersion = "20.1-forge"
    }
}
