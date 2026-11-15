import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `java-library`
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
    "testImplementation"(platform(catalog.findLibrary("junit-bom").get()))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}
