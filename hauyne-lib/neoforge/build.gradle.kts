plugins {
    id("mod")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

val mc = stonecutter.current.version

hauyneMetadata {
    neoforge {
        minecraftVersionRange = "[$mc]"
        loaderVersionRange = hauyneMinecraft.versionProperty("loaderVersionRange")
        javaFmlLoaderVersionRange = hauyneMinecraft.versionProperty("javaFmlLoaderVersionRange")
    }
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
