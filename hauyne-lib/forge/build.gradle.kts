plugins {
    id("mod")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.legacyforge)
}

val mc = stonecutter.current.version

hauyneMetadata {
    forge {
        minecraftVersionRange = "[$mc]"
        loaderVersionRange = stonecutter.properties.getLazy("loaderVersionRange")
        javaFmlLoaderVersionRange = stonecutter.properties.getLazy("javaFmlLoaderVersionRange")
    }
}

legacyForge {
    version = stonecutter.properties["forgeVersion"]

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
