package dev.lapis256.hauyne.config.nightconfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TomlTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun persistsValues() {
        val file = directory.resolve("config.toml")
        TomlBackend(file).use { backend ->
            backend.write("message", "hello")
        }
        TomlBackend(file).use { backend ->
            assertEquals("hello", backend.read("message"))
        }
    }
}
