# build-logic/src/main/kotlin/minecraft-library.gradle.kts

- sc · variable · L12-L12 — val sc = extensions.getByType<StonecutterBuildExtension>()
- javaVersion · variable · L15-L21 — val javaVersion = when { sc.current.parsed >= "26.1" -> 25 sc.current.parsed >= "1.20.5" -> 21 sc.current.parsed >= "1.18" -> 17 sc.current.parsed >= "1.17" -> 16 else -> 8 }
- artifact · variable · L27-L27 — val artifact = base.archivesName.get()
