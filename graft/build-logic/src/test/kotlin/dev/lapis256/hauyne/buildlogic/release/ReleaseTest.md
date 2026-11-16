# build-logic/src/test/kotlin/dev/lapis256/hauyne/buildlogic/release/ReleaseTest.kt

- ReleaseTest · class · L11-L132 — class ReleaseTest
- git · method · L15-L20 — private fun git(vararg args: String)
- prepareTask · method · L22-L34 — private fun prepareTask(): PrepareRelease
- initialReleaseSupportsEveryBump · method · L36-L46 — @Test fun initialReleaseSupportsEveryBump()
- autoBumpsAndSeparatesReleaseNotesFromHistory · method · L48-L67 — @Test fun autoBumpsAndSeparatesReleaseNotesFromHistory()
- integrityRejectsChangedMissingAndUnrecordedFiles · method · L69-L84 — @Test fun integrityRejectsChangedMissingAndUnrecordedFiles()
- rejectsMismatchedMavenPublication · method · L86-L99 — @Test fun rejectsMismatchedMavenPublication()
- recordsAnyRegisteredLoaderAndRejectsUnregisteredModFiles · method · L101-L131 — @Test fun recordsAnyRegisteredLoaderAndRejectsUnregisteredModFiles()
