plugins {
    id("hauyne.minecraft-library")
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.legacyforge)
}

legacyForge {
    version = hauyneMinecraft.versionProperty("forgeVersion").get()

    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    api(project(":hauyne-config:minecraft:${hauyneMinecraft.configMinecraftTarget.get()}"))
}

configurations.matching {
    it.name == "reobfApiElements"
}.configureEach {
    extendsFrom(configurations.getByName("api"))
}

configurations.matching {
    it.name == "reobfRuntimeElements"
}.configureEach {
    extendsFrom(configurations.getByName("implementation"), configurations.getByName("runtimeOnly"))
}
