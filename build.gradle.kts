plugins {
    base

    id("formatting")
    id("release")

    alias(libs.plugins.binaryCompatibilityValidator)
    alias(libs.plugins.dokka)

    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.legacyforge) apply false
    alias(libs.plugins.mdg) apply false
}

version = providers.gradleProperty("modVersion").get()

apiValidation {
    ignoredProjects.add("jvm")
}

dependencies {
    dokkaHtmlPlugin(libs.dokka.versioning)
}

dokka {
    moduleName = "HauyneLib"
    moduleVersion = project.version.toString()
    dokkaPublications.html {
        outputDirectory = layout.projectDirectory.dir("documentation/docs")
    }
    pluginsConfiguration.versioning {
        version = project.version.toString()
        olderVersionsDir = layout.projectDirectory.dir("documentation/version")
        renderVersionsNavigationOnAllPages = true
    }
}
