package dev.lapis256.hauyne.config.neoforge

import dev.lapis256.hauyne.config.Backend
import net.neoforged.neoforge.common.ModConfigSpec

class NeoforgeBackend(defaults: Map<String, String>) : Backend {
    private val builder = ModConfigSpec.Builder()
    private val values = defaults.mapValues { (key, value) -> builder.define(key, value) }

    @JvmField
    val spec: ModConfigSpec = builder.build()

    override fun read(key: String): String? = values[key]?.get()

    override fun write(key: String, value: String) {
        val entry = requireNotNull(values[key]) { "Unknown configuration key: $key" }
        entry.set(value)
        spec.save()
    }
}
