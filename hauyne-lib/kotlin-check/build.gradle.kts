plugins {
    id("minecraft-artifact")
    alias(libs.plugins.stonecutter)
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://maven.neoforged.net/releases")
    maven("https://libraries.minecraft.net")
}

dependencies {
    if (stonecutter.current.parsed < "1.20.5") {
        compileOnly("net.minecraftforge:forgespi:7.0.1")
        compileOnly("net.minecraftforge:mergetool:1.1.7:api")
    } else {
        compileOnly("net.neoforged.fancymodloader:loader:${stonecutter.properties["fancyModLoaderVersion"]}")
        compileOnly("net.neoforged:mergetool:2.0.0:api")
    }
    compileOnly("org.apache.maven:maven-artifact:3.9.9")
}

configurations.compileClasspath {
    isTransitive = false
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    manifest.attributes(
        "FMLModType" to "LIBRARY",
        "Implementation-Version" to providers.provider { project.version.toString() },
    )
}
