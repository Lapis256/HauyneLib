plugins {
    id("hauyne.library")
    id("hauyne.maven-publish")
}

dependencies {
    implementation(project(":hauyne-config:core"))
    implementation(libs.ksp.api)
}
