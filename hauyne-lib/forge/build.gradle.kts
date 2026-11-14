import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension

plugins {
    id("hauyne.mod-metadata")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.legacyforge)
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
    modPackage = "dev.lapis256.hauyne.lib.forge"
    license = providers.gradleProperty("license")
    this.loader = HauyneModLoader.FORGE
    minecraftVersionRange = "[$mc]"
    loaderVersionRange = hauyneMinecraft.versionProperty("loaderVersionRange")
    javaFmlLoaderVersionRange = hauyneMinecraft.versionProperty("javaFmlLoaderVersionRange")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(hauyneMinecraft.versionProperty("javaVersion").get().toInt())
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
    implementation(project(":hauyne-config:forge:$mc"))
    "jarJar"(project(":hauyne-config:core"))
    "jarJar"(project(":hauyne-config:minecraft:$mc"))
    "jarJar"(project(":hauyne-config:forge:$mc"))
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
