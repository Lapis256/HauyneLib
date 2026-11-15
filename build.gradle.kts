import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.lapis256.hauyne.buildlogic.minecraft.HauyneMinecraftExtension
import dev.lapis256.hauyne.buildlogic.minecraft.ValidateActiveProjects

plugins {
    base
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.stonecutter) apply false
    id("hauyne.formatting")
    id("hauyne.release")
}

version = providers.gradleProperty("modVersion").get()

val jvmModules = listOf(":hauyne-config:core", ":hauyne-config:ksp", ":hauyne-config:memory", ":hauyne-config:nightconfig", ":examples:jvm")
val publishedJvmModules = listOf(":hauyne-config:core", ":hauyne-config:ksp", ":hauyne-config:nightconfig")

val verifyJvm = tasks.register("verifyJvm") {
    dependsOn(jvmModules.map { "$it:build" })
}
val prepareJvmPublications = tasks.register("prepareJvmPublications") {
    dependsOn(publishedJvmModules.map { "$it:publishToMavenLocal" })
}
tasks.register("publishJvm") {
    dependsOn(publishedJvmModules.map { "$it:publish" })
}
val verifyRequired = tasks.named("verifyRequired") {
    dependsOn(verifyJvm, prepareJvmPublications)
}
val resetActiveProjects = tasks.register("resetActiveProjects")
val checkActiveProjects = tasks.register<ValidateActiveProjects>("checkActiveProjects")
val runActiveServer = tasks.register("runActiveServer")
val collect = tasks.register<Sync>("buildAndCollect") {
    dependsOn(verifyJvm, prepareJvmPublications)
    into(layout.buildDirectory.dir("collected"))
    for (module in publishedJvmModules) {
        from(layout.projectDirectory.dir("${module.trim(':').replace(':', '/')}/build/libs")) {
            val artifact = module.trim(':').replace(':', '-')
            val version = rootProject.version
            include("$artifact-$version.jar", "$artifact-$version-sources.jar")
            into("jvm")
        }
    }
}

val validateReleaseTargets = tasks.register("validateReleaseTargets")

gradle.projectsEvaluated {
    val controllers = allprojects.mapNotNull { controller ->
        controller.extensions.findByType<StonecutterControllerExtension>()?.let { controller to it }
    }.toMap()
    resetActiveProjects.configure {
        dependsOn(controllers.keys.map { "${it.path}:Reset active project" })
    }
    checkActiveProjects.configure {
        activeProjects = controllers.mapKeys { it.key.path }.mapValues { it.value.active?.project.orEmpty() }
        expectedProjects = controllers.mapKeys { it.key.path }.mapValues { it.value.vcsVersion.project }
    }
    for ((controller, _) in controllers) {
        for (target in controller.subprojects) {
            val minecraft = target.extensions.getByType<HauyneMinecraftExtension>()
            val verify = tasks.register("verify${target.path.replace(':', '_')}") {
                dependsOn("${target.path}:build", "${target.path}:publishToMavenLocal")
                if (target.tasks.findByName("publishMods") != null) dependsOn("${target.path}:publishMods")
            }
            tasks.named("ciCheck") { dependsOn(verify) }
            if (minecraft.releaseEnabled.get()) {
                verifyRequired.configure { dependsOn(verify) }
                if (target.tasks.findByName("validateModRelease") != null) {
                    validateReleaseTargets.configure { dependsOn("${target.path}:validateModRelease") }
                }
            }
            if (target.tasks.findByName("publishMods") != null) {
                collect.configure {
                    dependsOn("${target.path}:build")
                    from(target.layout.buildDirectory.dir("libs")) {
                        val artifact = "${target.extensions.getByType<BasePluginExtension>().archivesName.get()}-${target.version}"
                        include("$artifact.jar", "$artifact-sources.jar")
                        into("${controller.name}/${target.name}")
                    }
                }
            }
        }
    }
    val serverController = project(providers.gradleProperty("serverController").orElse(":hauyne-lib:neoforge").get())
    val selection = controllers.getValue(serverController)
    val active = selection.active?.project ?: selection.vcsVersion.project
    runActiveServer.configure {
        dependsOn("${serverController.path}:$active:runServer")
    }
}
