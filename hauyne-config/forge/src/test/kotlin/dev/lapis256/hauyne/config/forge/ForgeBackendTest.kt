package dev.lapis256.hauyne.config.forge

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ForgeBackendTest {
    @Test
    fun rejectsUnknownKeys() {
        val backend = ForgeBackend(mapOf("key" to "value"))
        assertThrows(IllegalArgumentException::class.java) { backend.write("unknown", "value") }
    }
}
