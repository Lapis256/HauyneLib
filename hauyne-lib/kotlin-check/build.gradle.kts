plugins {
    id("minecraft-artifact")
    alias(libs.plugins.stonecutter)
}

val usesMixin = stonecutter.current.parsed < "26.1"

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://maven.neoforged.net/releases")
    maven("https://libraries.minecraft.net")
}

dependencies {
    if (stonecutter.current.parsed < "1.20.5") {
        compileOnly(libs.forge.forgespi)
        compileOnly(libs.forge.fmlloader) {
            version { require(stonecutter.properties["forgeVersion"]) }
        }
        compileOnly(variantOf(libs.forge.mergetool) { classifier("api") })
    } else {
        compileOnly(libs.neoforge.fmlloader) {
            version { require(stonecutter.properties["fancyModLoaderVersion"]) }
        }
        compileOnly(variantOf(libs.neoforge.mergetool) { classifier("api") })
    }
    compileOnly(libs.maven.artifact)
    if (usesMixin) {
        compileOnly(libs.mixin)
        compileOnly(libs.asm.tree)
    }
}

if (usesMixin) {
    sourceSets.main {
        java.srcDir("src/legacy/java")
        resources.srcDir("src/legacy/resources")
    }
}

configurations.compileClasspath {
    isTransitive = false
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    if (usesMixin) {
        manifest.attributes("MixinConfigs" to "hauynelib-kotlin-check.mixins.json")
    }
    manifest.attributes(
        "FMLModType" to if (usesMixin) "GAMELIBRARY" else "LIBRARY",
        "Implementation-Version" to providers.provider { project.version.toString() },
    )
}
