# build-logic/src/main/kotlin/dev/lapis256/hauyne/buildlogic/metadata/FmlModMetadata.kt

- FmlModMetadata · class · L5-L13 — @Serializable internal data class FmlModMetadata( val loaderVersion: String, val license: String, val issueTrackerURL: String, val mods: List<FmlMod>, val dependencies: Map<String, List<FmlDependency>>, val modLoader: String = "javafml", )
- FmlMod · class · L15-L23 — @Serializable internal data class FmlMod( val modId: String, val version: String, val displayName: String, val description: String, val authors: String, val displayURL: String, )
- FmlDependency · class · L25-L34 — @Serializable internal data class FmlDependency( val modId: String, val versionRange: String, val mandatory: Boolean? = null, val type: String? = null, val ordering: String = "NONE", val side: String = "BOTH", val reason: String? = null, )
