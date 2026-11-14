# build-logic/src/main/kotlin/PrepareRelease.kt

- PrepareRelease · class · L12-L90 — abstract class PrepareRelease : DefaultTask()
- run · method · L22-L30 — private fun run(vararg args: String): String
- prepare · method · L32-L41 — @TaskAction fun prepare()
- currentVersion · method · L43-L47 — private fun currentVersion(source: String): String
- nextTag · method · L49-L58 — private fun nextTag(current: String, mode: String): String
- initialTag · method · L60-L69 — private fun initialTag(current: String, mode: String): String
- validateTag · method · L71-L74 — private fun validateTag(tag: String)
- generateChangelogs · method · L76-L80 — private fun generateChangelogs(root: File, tag: String)
- writeReleaseVersion · method · L82-L87 — private fun writeReleaseVersion(root: File, source: String, tag: String)
- gitCliff · method · L89-L89 — private fun gitCliff(vararg args: String): String
