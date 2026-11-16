# build-logic/src/main/kotlin/dev/lapis256/hauyne/buildlogic/release/RecordReleaseArtifacts.kt

- RecordReleaseArtifacts · class · L13-L72 — abstract class RecordReleaseArtifacts : DefaultTask()
- record · method · L26-L43 — @TaskAction fun record()
- copyVerifiedArtifacts · method · L45-L61 — private fun copyVerifiedArtifacts(root: File, repository: File, assets: File): Pair<List<String>, List<String>>
- writeChecksums · method · L63-L71 — private fun writeChecksums(root: File, entries: List<String>, checksums: List<String>)
