# build-logic/src/test/kotlin/ReleaseTest.kt

- ReleaseTest · class · L9-L126 — class ReleaseTest
- git · method · L13-L18 — private fun git(vararg args: String)
- prepareTask · method · L20-L32 — private fun prepareTask(): PrepareRelease
- initialReleaseSupportsEveryBump · method · L34-L43 — @Test fun initialReleaseSupportsEveryBump()
- autoBumpsAndSeparatesReleaseNotesFromHistory · method · L45-L63 — @Test fun autoBumpsAndSeparatesReleaseNotesFromHistory()
- integrityRejectsChangedMissingAndUnrecordedFiles · method · L65-L80 — @Test fun integrityRejectsChangedMissingAndUnrecordedFiles()
- settingsValidationCanOnlyBeSkippedWhenNotRequired · method · L82-L93 — @Test fun settingsValidationCanOnlyBeSkippedWhenNotRequired()
- recordsAnyRegisteredLoaderAndRejectsUnregisteredModFiles · method · L95-L125 — @Test fun recordsAnyRegisteredLoaderAndRejectsUnregisteredModFiles()
