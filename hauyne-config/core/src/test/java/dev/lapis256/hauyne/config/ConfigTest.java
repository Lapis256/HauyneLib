package dev.lapis256.hauyne.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void entryKeepsDefault() {
        var entry = new ConfigEntry<>("enabled", true);
        assertEquals("enabled", entry.getKey());
        assertTrue(entry.getDefault());
    }
}
