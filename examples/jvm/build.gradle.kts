plugins {
    id("hauyne.kotlin-library")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":hauyne-config:core"))
    ksp(project(":hauyne-config:ksp"))
}

kotlin {
    jvmToolchain(21)
}
