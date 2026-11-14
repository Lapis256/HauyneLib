# build-logic/src/main/kotlin/hauyne.library.gradle.kts

- library · variable · L17-L17 — val library = extensions.create<HauyneLibraryExtension>("hauyneLibrary")
- licenseFileName · variable · L34-L34 — val licenseFileName = "LICENSE_${rootProject.property("modId")}"
- catalog · variable · L47-L47 — val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
