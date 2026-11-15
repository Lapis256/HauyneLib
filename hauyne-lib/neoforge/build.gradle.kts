plugins {
    id("mod")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

val mc = stonecutter.current.version

hauyneMetadata {
    neoforge {
        minecraftVersionRange = "[$mc]"
        loaderVersionRange = stonecutter.properties.getLazy("loaderVersionRange")
        javaFmlLoaderVersionRange = stonecutter.properties.getLazy("javaFmlLoaderVersionRange")
    }
}

neoForge {
    version = stonecutter.properties["neoForgeVersion"]

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
