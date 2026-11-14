package dev.lapis256.hauyne.config.neoforge;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NeoforgeBackendTest {

    @Test
    void verifiesBehavior() {
        var backend = new NeoforgeBackend(java.util.Map.of("key", "value"));
        assertNotNull(backend.spec);
        assertThrows(IllegalArgumentException.class, () -> backend.write("unknown", "value"));
    }
}
