plugins {
    `kotlin-dsl`
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
}
