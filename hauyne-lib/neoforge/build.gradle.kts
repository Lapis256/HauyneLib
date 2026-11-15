import dev.lapis256.hauyne.buildlogic.metadata.HauyneFmlLoader

plugins {
    id("hauyne.mod")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
    id("hauyne.mod-publish")
}

val mc = stonecutter.current.version

hauyneFmlMetadata {
    loader = HauyneFmlLoader.NEOFORGE
    minecraftVersionRange = "[$mc]"
    loaderVersionRange = hauyneMinecraft.versionProperty("loaderVersionRange")
    javaFmlLoaderVersionRange = hauyneMinecraft.versionProperty("javaFmlLoaderVersionRange")
}

neoForge {
    version = hauyneMinecraft.versionProperty("neoForgeVersion").get()

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
