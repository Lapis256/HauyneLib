plugins {
    id("hauyne.java-library")
    id("org.jetbrains.kotlin.jvm")
}

// Java's sourcesJar includes Kotlin sources; avoid two tasks writing the same archive.
tasks.matching { it.name == "kotlinSourcesJar" }.configureEach {
    enabled = false
}
