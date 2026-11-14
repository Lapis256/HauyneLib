# build-logic/src/main/kotlin/hauyne.mod-metadata.gradle.kts

- metadata · variable · L5-L5 — val metadata = extensions.create<HauyneModMetadataExtension>("hauyneModMetadata")
- constants · variable · L6-L10 — val constants = tasks.register<GenerateModConstants>("generateModConstants") { modId = metadata.modId modPackage = metadata.modPackage outputDirectory = layout.buildDirectory.dir("generated/sources/modConstants") }
- loader · variable · L21-L21 — val loader = metadata.loader.get()
- dependencies · variable · L22-L35 — val dependencies = listOf( "minecraft" to metadata.minecraftVersionRange.get(), (if (loader == HauyneModLoader.FORGE) "forge" else "neoforge") to metadata.loaderVersionRange.get(), ).joinToString("\n") { (id, range) -> """ [[dependencies.${metadata.modId.get()}]] modId="$id" versionRange="$range" ${if (loader == HauyneModLoader.FORGE) "mandatory=true" else "type=\"required\""} ordering="NONE" side="BOTH" """.trimIndent() }
- values · variable · L37-L44 — val values = mapOf( "modId" to metadata.modId.get(), "modName" to metadata.modName.get(), "version" to project.version.toString(), "dependencies" to dependencies, "loaderVersion" to metadata.javaFmlLoaderVersionRange.get(), "license" to metadata.license.get(), )
