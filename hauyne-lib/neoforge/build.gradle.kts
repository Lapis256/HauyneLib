import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("hauyne.mod-metadata")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.mdg)
    id("hauyne.mod-publish")
}

val mc = stonecutter.current.version

hauyneLibrary {
    componentPath = parent!!.path
}

hauyneMinecraft {
    minecraftVersion = mc
    versionPropertiesFile = rootProject.layout.projectDirectory.file("versions/$mc/gradle.properties")
}

hauyneModMetadata {
    modId = providers.gradleProperty("modId")
    modName = providers.gradleProperty("modName")
    modPackage = "dev.lapis256.hauyne.lib.neoforge"
    license = providers.gradleProperty("license")
    this.loader = HauyneModLoader.NEOFORGE
    minecraftVersionRange = "[$mc]"
    loaderVersionRange = hauyneMinecraft.versionProperty("loaderVersionRange")
    javaFmlLoaderVersionRange = hauyneMinecraft.versionProperty("javaFmlLoaderVersionRange")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(hauyneMinecraft.versionProperty("javaVersion").get().toInt())
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
    implementation(project(":hauyne-config:neoforge:$mc"))
    "jarJar"(project(":hauyne-config:core"))
    "jarJar"(project(":hauyne-config:minecraft:$mc"))
    "jarJar"(project(":hauyne-config:neoforge:$mc"))
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
