import dev.lapis256.hauyne.buildlogic.metadata.HauyneFmlLoader
import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("hauyne.fml-metadata")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
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
    loader = HauyneFmlLoader.NEOFORGE
    minecraftVersionRange = "[$mc]"
    loaderVersionRange = hauyneMinecraft.versionProperty("loaderVersionRange")
    javaFmlLoaderVersionRange = hauyneMinecraft.versionProperty("javaFmlLoaderVersionRange")
}

extensions.configure<NeoForgeExtension> {
    enable {
        version = hauyneMinecraft.versionProperty("neoForgeVersion").get()
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
    implementation(project(":hauyne-config:neoforge:${stonecutter.current.project}"))
    "jarJar"(project(":hauyne-config:core"))
    "jarJar"(project(":hauyne-config:minecraft:${stonecutter.current.project}"))
    "jarJar"(project(":hauyne-config:neoforge:${stonecutter.current.project}"))
}

publishMods {
    file = tasks.named<Jar>("jar").flatMap {
        it.archiveFile
    }

    modLoaders.add("neoforge")
    curseforge {
        minecraftVersions.add(mc)
    }

    modrinth {
        minecraftVersions.add(mc)
    }
}
