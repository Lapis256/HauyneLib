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

val targets = java.util.Properties().apply {
    load(providers.fileContents(layout.settingsDirectory.file("versions/targets.properties")).asText.get().reader())
}
val defaultTarget = requireNotNull(targets.getProperty("default"))
val targetVersions = targets.stringPropertyNames().filter { it != "default" }.sortedDescending().associateWith {
    val fields = targets.getProperty(it).split(',')
    require(fields.size == 3 && fields[1] in listOf("forge", "neoforge")) { "Invalid target: $it" }
    fields[2].toBooleanStrict()
    fields
}
require(defaultTarget in targetVersions) { "Unknown default target: $defaultTarget" }

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"
    create(":hauyne-config:minecraft") {
        targetVersions.forEach { (series, fields) -> version(series, fields[0]) }
        vcsVersion = defaultTarget
    }

    for (loader in listOf("neoforge", "forge")) {
        val supported = targetVersions.filterValues { it[1] == loader }
        for (path in listOf(":hauyne-config:$loader", ":hauyne-lib:$loader")) {
            create(path) {
                supported.forEach { (series, fields) -> version(series, fields[0]) }
                vcsVersion = defaultTarget.takeIf { it in supported } ?: supported.keys.first()
            }
        }
    }
}
