import dev.lapis256.hauyne.buildlogic.library.HauyneLibraryExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
}

group = rootProject.property("mavenGroup")!!
version = rootProject.version
val library = extensions.create<HauyneLibraryExtension>("hauyneLibrary")
library.componentPath.convention(project.path)

base {
    archivesName = library.componentPath.map {
        it.trim(':').replace(':', '-')
    }
}

java {
    toolchain.languageVersion.convention(JavaLanguageVersion.of(17))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

val licenseFileName = "LICENSE_${rootProject.property("modId")}"

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        rename("LICENSE", licenseFileName)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    // BCV 付属の ASM 9.6 では Java 25 のクラスを読み取れない。
    "bcv-rt-jvm-cp"(catalog.findLibrary("asm-tree").get())
    "testImplementation"(platform(catalog.findLibrary("junit-bom").get()))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

// Java's sourcesJar includes Kotlin sources; avoid two tasks writing the same archive.
tasks.matching { it.name == "kotlinSourcesJar" }.configureEach {
    enabled = false
}
