import dev.lapis256.hauyne.buildlogic.metadata.gte

plugins {
    id("mod")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.legacyforge)
}

val mc = stonecutter.current.version
val kotlinCheckMixinConfig = "hauynelib-kotlin-check.mixins.json"

sourceSets.main {
    val sharedSources = rootProject.layout.projectDirectory.dir("hauyne-lib/src/main")
    java.srcDir(sharedSources.dir("java"))
    resources.srcDir(sharedSources.dir("resources"))
}

tasks.withType<Jar>().configureEach {
    manifest.attributes("MixinConfigs" to kotlinCheckMixinConfig)
}

hauyneMetadata {
    forge {
        loaderVersionRange = stonecutter.properties.getLazy("minimumLoaderVersion").gte()
        javaFmlLoaderVersionRange = stonecutter.properties.getLazy("minimumJavaFmlLoaderVersion").gte()
    }
}

legacyForge {
    version = stonecutter.properties["forgeVersion"]

    addModdingDependenciesTo(sourceSets.test.get())
    runs {
        // 開発ランは JAR のマニフェストを経由しないため、起動引数で同じ設定を渡す。
        configureEach {
            programArgument("--mixin.config")
            programArgument(kotlinCheckMixinConfig)
        }
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
