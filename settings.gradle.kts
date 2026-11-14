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
        versions("26.1.2", "1.21.1", "1.20.1")
        vcsVersion = "26.1.2"
    }

    for (path in listOf(":hauyne-config:neoforge", ":hauyne-lib:neoforge")) {
        create(path) {
            versions("26.1.2", "1.21.1")
            vcsVersion = "26.1.2"
        }
    }

    for (path in listOf(":hauyne-config:forge", ":hauyne-lib:forge")) {
        create(path) {
            versions("1.20.1")
            vcsVersion = "1.20.1"
        }
    }
}
