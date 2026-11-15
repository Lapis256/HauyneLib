package dev.lapis256.hauyne.config.forge

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ForgeBackendTest {
    @Test
    fun verifiesBehavior() {
        val backend = ForgeBackend(mapOf("key" to "value"))
        assertNotNull(backend.spec)
        assertThrows(IllegalArgumentException::class.java) { backend.write("unknown", "value") }
    }
}
