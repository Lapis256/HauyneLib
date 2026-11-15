import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.lapis256.hauyne.buildlogic.minecraft.HauyneTargets
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

val targets = extensions.getByType<HauyneTargets>()
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
val checkActiveSelection = tasks.register<ValidateActiveProjects>("checkActiveSelection")
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

for (target in targets.all) {
    val verify = tasks.register("verify${target.series}") {
        dependsOn(":hauyne-config:core:publishToMavenLocal")
        for (module in target.modules) {
            dependsOn("$module:${target.series}:build", "$module:${target.series}:publishToMavenLocal")
        }
        dependsOn(":hauyne-lib:${target.loader}:${target.series}:publishMods")
    }
    if (target.release) verifyRequired.configure { dependsOn(verify) }
    tasks.named("ciCheck") { dependsOn(verify) }
    tasks.register("switchTo${target.series}") {
        dependsOn(target.modules.map { "$it:Set active project to ${target.series}" })
    }
    tasks.register("publishMaven${target.series}") {
        dependsOn(target.modules.map { "$it:${target.series}:publish" })
    }
    tasks.register("publishMods${target.series}") {
        dependsOn(":hauyne-lib:${target.loader}:${target.series}:publishMods")
    }
    collect.configure {
        dependsOn(":hauyne-lib:${target.loader}:${target.series}:build")
        from(layout.projectDirectory.dir("hauyne-lib/${target.loader}/versions/${target.series}/build/libs")) {
            val artifact = "hauyne-lib-${target.loader}-${target.series}-${rootProject.version}"
            include("$artifact.jar", "$artifact-sources.jar")
            into("${target.loader}/${target.series}")
        }
    }
}

tasks.register("validateReleaseTargets") {
    dependsOn(targets.all.filter { it.release }.map { ":hauyne-lib:${it.loader}:${it.series}:validateModRelease" })
}

gradle.projectsEvaluated {
    val controllers = targets.all.flatMap { it.modules }.distinct().associateWith {
        project(it).extensions.getByType<StonecutterControllerExtension>()
    }
    resetActiveProjects.configure {
        dependsOn(controllers.keys.map { "$it:Reset active project" })
    }
    checkActiveProjects.configure {
        activeProjects = controllers.mapValues { it.value.active?.project.orEmpty() }
        expectedProjects = controllers.mapValues { it.value.vcsVersion.project }
    }
    val active = controllers.getValue(":hauyne-config:minecraft").active?.project
    val target = targets.all.single { it.series == (active ?: targets.default) }
    checkActiveSelection.configure {
        activeProjects = target.modules.associateWith { controllers.getValue(it).active?.project.orEmpty() }
        expectedProjects = target.modules.associateWith { target.series }
    }
    val server = project(":hauyne-lib:${target.loader}:${target.series}").tasks.named("runServer")
    server.configure { mustRunAfter(checkActiveSelection) }
    runActiveServer.configure {
        dependsOn(checkActiveSelection, server)
    }
}
