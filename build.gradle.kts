plugins {
    base
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    id("hauyne.formatting")
}

tasks.register("verifyRequired") {
    for (module in listOf(
        ":hauyne-config:core",
        ":hauyne-config:ksp",
        ":hauyne-config:memory",
        ":hauyne-config:nightconfig",
        ":examples:jvm",
    )) {
        dependsOn("$module:build")
    }

    for (module in listOf(":hauyne-config:core", ":hauyne-config:ksp", ":hauyne-config:nightconfig")) {
        dependsOn("$module:publishToMavenLocal")
    }

    for (mc in listOf("26.1.2", "1.21.1")) {
        for (module in listOf(":hauyne-config:minecraft", ":hauyne-config:neoforge", ":hauyne-lib:neoforge")) {
            dependsOn("$module:$mc:build", "$module:$mc:publishToMavenLocal")
        }

        dependsOn(":hauyne-lib:neoforge:$mc:publishMods")
    }
}
