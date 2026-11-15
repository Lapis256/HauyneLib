import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension

plugins {
    id("hauyne.fml-metadata")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.legacyforge)
    id("hauyne.mod-publish")
}

val mc = stonecutter.current.version

hauyneModMetadata {
    modId = providers.gradleProperty("modId")
    modName = providers.gradleProperty("modName")
    modPackage = "dev.lapis256.hauyne.lib"
    license = providers.gradleProperty("license")
}

hauyneFmlMetadata {
    loader = HauyneFmlLoader.FORGE
    minecraftVersionRange = "[$mc]"
    loaderVersionRange = hauyneMinecraft.versionProperty("loaderVersionRange")
    javaFmlLoaderVersionRange = hauyneMinecraft.versionProperty("javaFmlLoaderVersionRange")
}

extensions.configure<LegacyForgeExtension> {
    enable {
        forgeVersion = hauyneMinecraft.versionProperty("forgeVersion").get()
    }

    addModdingDependenciesTo(sourceSets.test.get())
    runs {
        create("server") {
            server()
            programArgument("--nogui")
        }
    }

    mods {
        create(rootProject.property("modId").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":hauyne-config:forge:${stonecutter.current.project}"))
    "jarJar"(project(":hauyne-config:core"))
    "jarJar"(project(":hauyne-config:minecraft:${stonecutter.current.project}"))
    "jarJar"(project(":hauyne-config:forge:${stonecutter.current.project}"))
}

publishMods {
    file = tasks.named<net.neoforged.moddevgradle.legacyforge.tasks.RemapJar>("reobfJar").flatMap {
        it.archiveFile
    }

    modLoaders.add("forge")
    curseforge {
        minecraftVersions.add(mc)
    }

    modrinth {
        minecraftVersions.add(mc)
    }
}

configurations.matching {
    it.name == "reobfApiElements"
}.configureEach {
    extendsFrom(configurations.getByName("api"))
}

configurations.matching {
    it.name == "reobfRuntimeElements"
}.configureEach {
    extendsFrom(configurations.getByName("implementation"), configurations.getByName("runtimeOnly"))
}
