package dev.lapis256.hauyne.config.neoforge

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NeoforgeBackendTest {
    @Test
    fun rejectsUnknownKeys() {
        val backend = NeoforgeBackend(mapOf("key" to "value"))
        assertThrows(IllegalArgumentException::class.java) { backend.write("unknown", "value") }
    }
}
