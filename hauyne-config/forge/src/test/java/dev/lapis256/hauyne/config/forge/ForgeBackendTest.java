package dev.lapis256.hauyne.config.forge;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForgeBackendTest {

    @Test
    void verifiesBehavior() {
        var backend = new ForgeBackend(java.util.Map.of("key", "value"));
        assertNotNull(backend.spec);
        assertThrows(IllegalArgumentException.class, () -> backend.write("unknown", "value"));
    }
}
