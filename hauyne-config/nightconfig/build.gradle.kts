plugins {
    id("library")
    id("maven-publishing")
}

dependencies {
    api(project(":hauyne-config:core"))
    implementation(libs.nightconfig)
}
