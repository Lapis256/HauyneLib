plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":hauyne-config:core"))
    ksp(project(":hauyne-config:ksp"))
}

kotlin {
    jvmToolchain(21)
}
