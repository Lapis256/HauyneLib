package dev.lapis256.hauyne.config.memory

import dev.lapis256.hauyne.config.Backend

class MemoryBackend : Backend {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String) = values[key]

    override fun write(key: String, value: String) {
        values[key] = value
    }
}
