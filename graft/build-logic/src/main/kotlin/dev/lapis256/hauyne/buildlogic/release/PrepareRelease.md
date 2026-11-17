# build-logic/src/main/kotlin/dev/lapis256/hauyne/buildlogic/release/PrepareRelease.kt

- PrepareRelease · class · L14-L92 — abstract class PrepareRelease : DefaultTask()
- run · method · L24-L32 — private fun run(vararg args: String): String
- prepare · method · L34-L43 — @TaskAction fun prepare()
- currentVersion · method · L45-L49 — private fun currentVersion(source: String): String
- nextTag · method · L51-L60 — private fun nextTag(current: String, mode: String): String
- initialTag · method · L62-L71 — private fun initialTag(current: String, mode: String): String
- validateTag · method · L73-L76 — private fun validateTag(tag: String)
- generateChangelogs · method · L78-L82 — private fun generateChangelogs(root: File, tag: String)
- writeReleaseVersion · method · L84-L89 — private fun writeReleaseVersion(root: File, source: String, tag: String)
- gitCliff · method · L91-L91 — private fun gitCliff(vararg args: String): String
