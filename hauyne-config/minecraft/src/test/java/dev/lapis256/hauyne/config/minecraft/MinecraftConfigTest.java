package dev.lapis256.hauyne.config.minecraft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinecraftConfigTest {

    @Test
    void verifiesBehavior() {
        assertEquals("1, 2, 3", MinecraftConfig.describe(new net.minecraft.core.BlockPos(1, 2, 3)));
    }
}
