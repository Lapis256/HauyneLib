import dev.lapis256.hauyne.buildlogic.metadata.HauyneFmlLoader

plugins {
    id("mod")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.legacyforge)
}

val mc = stonecutter.current.version

hauyneMetadata {
    forgeLike {
        loader = HauyneFmlLoader.FORGE
        minecraftVersionRange = "[$mc]"
        loaderVersionRange = hauyneMinecraft.versionProperty("loaderVersionRange")
        javaFmlLoaderVersionRange = hauyneMinecraft.versionProperty("javaFmlLoaderVersionRange")
    }
}

legacyForge {
    version = hauyneMinecraft.versionProperty("forgeVersion").get()

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
