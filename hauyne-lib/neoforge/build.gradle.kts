import dev.lapis256.hauyne.buildlogic.metadata.gte

plugins {
    id("mod")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

val mc = stonecutter.current.version

hauyneMetadata {
    neoforge {
        loaderVersionRange = stonecutter.properties.getLazy("minimumLoaderVersion").gte()
        javaFmlLoaderVersionRange = stonecutter.properties.getLazy("minimumJavaFmlLoaderVersion").gte()
    }
}

neoForge {
    version = stonecutter.properties["neoForgeVersion"]

    addModdingDependenciesTo(sourceSets.test.get())
    runs {
        create("server") {
            server()
            gameDirectory = layout.projectDirectory.dir("run-server")
        }
    }

    mods {
        create(rootProject.property("modId").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }
}

publishMods {
    modLoaders.add("neoforge")
    curseforge {
        minecraftVersions.add(mc)
    }

    modrinth {
        minecraftVersions.add(mc)
    }
}
