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
        compileOnly("net.minecraftforge:forgespi:7.0.1")
        compileOnly("net.minecraftforge:fmlloader:${stonecutter.properties["forgeVersion"]}")
        compileOnly("net.minecraftforge:mergetool:1.1.7:api")
    } else {
        compileOnly("net.neoforged.fancymodloader:loader:${stonecutter.properties["fancyModLoaderVersion"]}")
        compileOnly("net.neoforged:mergetool:2.0.0:api")
    }
    compileOnly("org.apache.maven:maven-artifact:3.9.9")
    if (usesMixin) {
        compileOnly("org.spongepowered:mixin:0.8.5")
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
