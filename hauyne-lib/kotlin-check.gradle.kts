plugins {
    `java-library`
}

group = "dev.lapis256.hauyne"
version = rootProject.version

val variant = projectDir.name

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
    java.srcDir("../common/src/main/java")
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    manifest.attributes(
        "Automatic-Module-Name" to "dev.lapis256.hauyne.kotlincheck",
        "FMLModType" to "LIBRARY",
        "Implementation-Version" to project.version,
    )
}
