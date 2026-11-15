plugins {
    id("hauyne.fml-metadata")
}

val configBackend = project(":hauyne-config:${parent!!.name}:${project.name}")
val configMinecraft = project(":hauyne-config:minecraft:${project.name}")

dependencies {
    implementation(configBackend)
}

for (loaderPlugin in listOf("net.neoforged.moddev", "net.neoforged.moddev.legacyforge")) {
    pluginManager.withPlugin(loaderPlugin) {
        dependencies {
            add("jarJar", project(":hauyne-config:core"))
            add("jarJar", configMinecraft)
            add("jarJar", configBackend)
        }
    }
}
