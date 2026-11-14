package dev.lapis256.hauyne.lib.neoforge;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModTest {

    @Test
    void metadataIsPackaged() {
        assertNotNull(getClass().getClassLoader().getResource("META-INF/neoforge.mods.toml"));
    }
}
