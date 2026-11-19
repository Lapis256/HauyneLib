plugins {
    `java-library`
    alias(libs.plugins.stonecutter)
}

group = "dev.lapis256.hauyne"
version = "${project.name}-${rootProject.version}"

val variant = when (stonecutter.current.version) {
    "1.20.1" -> "forge"
    "1.21.1" -> "neoforge21"
    "26.1.2" -> "neoforge26"
    else -> error("Unsupported Minecraft version: ${stonecutter.current.version}")
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://maven.neoforged.net/releases")
    maven("https://libraries.minecraft.net")
}

dependencies {
    when (variant) {
        "forge" -> {
            compileOnly("net.minecraftforge:forgespi:7.0.1")
            compileOnly("net.minecraftforge:mergetool:1.1.7:api")
        }

        "neoforge21" -> compileOnly("net.neoforged.fancymodloader:loader:4.0.45")

        else -> compileOnly("net.neoforged.fancymodloader:loader:11.0.15")
    }
    compileOnly("org.apache.maven:maven-artifact:3.9.9")
}

configurations.compileClasspath {
    isTransitive = false
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(
        when (variant) {
            "forge" -> 17
            "neoforge21" -> 21
            else -> 25
        },
    )
}

sourceSets.main {
    val variantSources = rootProject.layout.projectDirectory.dir("hauyne-lib/kotlin-check/src/$variant")
    java.srcDir(variantSources.dir("java"))
    resources.srcDir(variantSources.dir("resources"))
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    manifest.attributes(
        "Automatic-Module-Name" to "dev.lapis256.hauyne.kotlincheck",
        "FMLModType" to "LIBRARY",
        "Implementation-Version" to project.version,
    )
}
