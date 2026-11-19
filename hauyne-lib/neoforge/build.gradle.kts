import dev.lapis256.hauyne.buildlogic.metadata.GenerateFmlModMetadata
import dev.lapis256.hauyne.buildlogic.metadata.gte

plugins {
    id("mod")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
}

val mc = stonecutter.current.version

if (mc == "1.21.1") {
    sourceSets.main {
        val sharedSources = rootProject.layout.projectDirectory.dir("hauyne-lib/src/main")
        java.srcDir(sharedSources.dir("java"))
        resources.srcDir(sharedSources.dir("resources"))
    }

    tasks.named<GenerateFmlModMetadata>("generateModMetadata") {
        mixinConfig = "hauynelib-kotlin-check.mixins.json"
    }
}

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
