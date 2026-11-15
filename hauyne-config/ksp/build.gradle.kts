plugins {
    id("library")
    id("maven-publishing")
}

dependencies {
    implementation(project(":hauyne-config:core"))
    implementation(libs.ksp.api)
}
