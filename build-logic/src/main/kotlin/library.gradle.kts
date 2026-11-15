import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

group = rootProject.property("mavenGroup")!!
version = rootProject.version

rootProject.dependencies.add("dokka", project)

dokka {
    moduleName = project.path.trim(':').replace(':', '-')
    moduleVersion = rootProject.version.toString()
    dokkaSourceSets.configureEach {
        // 非アクティブな Minecraft バージョンの Stonecutter 生成ソースも対象にする。
        suppressGeneratedFiles = false
        documentedVisibilities.set(VisibilityModifier.values().toSet())
        skipDeprecated = false
        enableAndroidDocumentationLink = false
        val sourceUrl = "${rootProject.property("projectUrl")}/tree/${providers.gradleProperty("docsRef").getOrElse("main")}"
        if (projectDir.parentFile.name == "versions") {
            sourceLink {
                localDirectory = layout.buildDirectory.dir("generated/stonecutter/main")
                remoteUrl = uri("$sourceUrl/${parent!!.path.trim(':').replace(':', '/')}/src/main")
                remoteLineSuffix = "#L"
            }
        }
        sourceLink {
            localDirectory = rootProject.projectDir
            remoteUrl = uri(sourceUrl)
            remoteLineSuffix = "#L"
        }
    }
}

base {
    archivesName = project.path.trim(':').replace(':', '-')
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
