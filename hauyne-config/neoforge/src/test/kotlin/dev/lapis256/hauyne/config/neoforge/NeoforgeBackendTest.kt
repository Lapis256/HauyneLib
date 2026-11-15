package dev.lapis256.hauyne.config.neoforge

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NeoforgeBackendTest {
    @Test
    fun verifiesBehavior() {
        val backend = NeoforgeBackend(mapOf("key" to "value"))
        assertNotNull(backend.spec)
        assertThrows(IllegalArgumentException::class.java) { backend.write("unknown", "value") }
    }
}
