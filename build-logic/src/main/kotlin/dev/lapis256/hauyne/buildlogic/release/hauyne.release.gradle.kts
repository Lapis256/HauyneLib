import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.lapis256.hauyne.buildlogic.publishing.ValidateReleaseSettings
import dev.lapis256.hauyne.buildlogic.release.PrepareRelease
import dev.lapis256.hauyne.buildlogic.release.RecordReleaseArtifacts

tasks.register("verifyRequired") {
    group = "verification"
    description = "Build, test, publish locally and dry-run all registered release targets"
}

tasks.register("ciCheck") {
    group = "verification"
    description = "Run CI checks for all supported targets"
    dependsOn("spotlessCheck", "verifyRequired")
}

val validateRelease = tasks.register<ValidateReleaseSettings>("validateRelease") {
    group = "publishing"
    description = "Validate all publication settings before uploading"
    required = providers.environmentVariable("CI").map(String::toBooleanStrict).orElse(false)
    for (key in listOf("R2_ACCESS_KEY", "R2_SECRET_KEY", "R2_ENDPOINT", "CURSEFORGE_TOKEN", "MODRINTH_TOKEN")) {
        configurationPresent.put(key, providers.environmentVariable(key).map { it.isNotBlank() }.orElse(false))
    }
    for (key in listOf("curseforgeProjectId", "modrinthProjectId")) {
        configurationPresent.put(key, providers.gradleProperty(key).map { it.isNotBlank() }.orElse(false))
    }
}

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publish the verified release targets to Maven, CurseForge and Modrinth"
    dependsOn(validateRelease)
}

tasks.register<PrepareRelease>("prepareRelease") {
    group = "publishing"
    description = "Update the release version and generate both changelogs with git-cliff"
    rootDirectory = layout.projectDirectory
    bump = providers.gradleProperty("releaseBump").orElse("auto")
}

val localMaven = repositories.mavenLocal().url

tasks.register<RecordReleaseArtifacts>("recordReleaseArtifacts") {
    group = "publishing"
    description = "Compare and record the exact locally verified publication files"
    rootDirectory = layout.projectDirectory
    localRepository = file(localMaven)
    publicationFiles.convention(emptyMap())
    dependsOn("verifyRequired")
}

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
    for ((controller, _) in controllers) {
        for (target in controller.subprojects) {
            val verify = tasks.register("verify${target.path.replace(':', '_')}") {
                dependsOn("${target.path}:build", "${target.path}:publishToMavenLocal")
                if (target.tasks.findByName("publishMods") != null) dependsOn("${target.path}:publishMods")
            }
            tasks.named("ciCheck") { dependsOn(verify) }
            verifyRequired.configure { dependsOn(verify) }
            if (target.tasks.findByName("validateModRelease") != null) {
                validateReleaseTargets.configure { dependsOn("${target.path}:validateModRelease") }
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
}
