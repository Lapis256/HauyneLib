# build-logic/src/main/kotlin/RecordReleaseArtifacts.kt

- RecordReleaseArtifacts · class · L11-L70 — abstract class RecordReleaseArtifacts : DefaultTask()
- record · method · L24-L41 — @TaskAction fun record()
- copyVerifiedArtifacts · method · L43-L59 — private fun copyVerifiedArtifacts(root: File, repository: File, assets: File): Pair<List<String>, List<String>>
- writeChecksums · method · L61-L69 — private fun writeChecksums(root: File, entries: List<String>, checksums: List<String>)
