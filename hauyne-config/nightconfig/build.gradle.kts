plugins {
    id("hauyne.library")
    id("hauyne.maven-publish")
}

dependencies {
    api(project(":hauyne-config:core"))
    implementation(libs.nightconfig)
}
