plugins {
    `java-library`
    alias(libs.plugins.stonecutter)
}

base {
    archivesName = parent!!.path.trim(':').replace(':', '-')
}

group = "${rootProject.property("mavenGroup")}.hauyne.lib.kotlin.check"
version = "${project.name}-${rootProject.version}"

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
    }
    compileOnly("org.apache.maven:maven-artifact:3.9.9")
}

configurations.compileClasspath {
    isTransitive = false
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(
        when {
            stonecutter.current.parsed >= "26.1" -> 25
            stonecutter.current.parsed >= "1.20.5" -> 21
            else -> 17
        },
    )
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    manifest.attributes(
        "Automatic-Module-Name" to "dev.lapis256.hauyne.kotlincheck",
        "FMLModType" to "LIBRARY",
        "Implementation-Version" to project.version,
    )
}
