plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotless.gradle)
    implementation(libs.ec4j)
    implementation(libs.kotlin.gradle)
    implementation(libs.modpublish.gradle)
    implementation(libs.stonecutter.gradle)
    implementation(libs.ktoml)
    testImplementation(libs.nightconfig)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
