plugins {
    id("hauyne.minecraft-library")
}

dependencies {
    api(project(":hauyne-config:minecraft:${project.name}"))
}
