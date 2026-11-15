plugins {
    base
    alias(libs.plugins.binaryCompatibilityValidator)
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    id("formatting")
    id("release")
}

version = providers.gradleProperty("modVersion").get()

apiValidation {
    ignoredProjects.add("jvm")
}
