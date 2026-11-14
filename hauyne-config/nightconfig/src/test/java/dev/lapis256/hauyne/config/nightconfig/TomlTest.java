package dev.lapis256.hauyne.config.nightconfig;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TomlTest {
    @TempDir
    Path directory;

    @Test
    void persistsValues() {
        Path file = directory.resolve("config.toml");
        try (var backend = new TomlBackend(file)) {
            backend.write("message", "hello");
        }

        try (var backend = new TomlBackend(file)) {
            assertEquals("hello", backend.read("message"));
        }
    }
}
