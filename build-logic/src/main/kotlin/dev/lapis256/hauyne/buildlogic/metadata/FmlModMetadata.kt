package dev.lapis256.hauyne.buildlogic.metadata

import kotlinx.serialization.Serializable

@Serializable
internal data class FmlModMetadata(
    val loaderVersion: String,
    val license: String,
    val issueTrackerURL: String,
    val mods: List<FmlMod>,
    val dependencies: Map<String, List<FmlDependency>>,
    val features: Map<String, Map<String, String>>? = null,
    val mixins: List<FmlMixin>? = null,
    val modLoader: String = "javafml",
)

@Serializable
internal data class FmlMixin(val config: String)

@Serializable
internal data class FmlMod(
    val modId: String,
    val version: String,
    val displayName: String,
    val description: String,
    val authors: String,
    val displayURL: String,
)

@Serializable
internal data class FmlDependency(
    val modId: String,
    val versionRange: String,
    val mandatory: Boolean? = null,
    val type: String? = null,
    val ordering: String = "NONE",
    val side: String = "BOTH",
    val reason: String? = null,
)
