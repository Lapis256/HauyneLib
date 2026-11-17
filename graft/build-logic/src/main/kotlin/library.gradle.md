# build-logic/src/main/kotlin/library.gradle.kts

- sourceUrl · variable · L30-L30 — val sourceUrl = "${rootProject.property("projectUrl")}/tree/${providers.gradleProperty("docsRef").getOrElse("main")}"
- licenseFileName · variable · L59-L59 — val licenseFileName = "LICENSE_${rootProject.property("modId")}"
- catalog · variable · L72-L72 — val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
- artifactVersion · variable · L75-L75 — val artifactVersion = providers.provider { project.version.toString() }
- artifactName · variable · L76-L76 — val artifactName = base.archivesName.get()
- compilerMetadata · variable · L77-L77 — val compilerMetadata = javaToolchains.compilerFor(java.toolchain).map { it.metadata }
- timestamp · variable · L78-L78 — val timestamp = OffsetDateTime.now()
