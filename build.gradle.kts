plugins {
    base
    alias(libs.plugins.binaryCompatibilityValidator)
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    id("formatting")
    id("release")
    alias(libs.plugins.dokka)
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
