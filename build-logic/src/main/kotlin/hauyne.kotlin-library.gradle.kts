plugins {
    id("hauyne.library")
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

// Java's sourcesJar includes Kotlin sources; avoid two tasks writing the same archive.
tasks.matching { it.name == "kotlinSourcesJar" }.configureEach {
    enabled = false
}
