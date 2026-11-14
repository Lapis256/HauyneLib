package dev.lapis256.hauyne.config.minecraft;

import dev.lapis256.hauyne.config.Backend;

public final class MinecraftConfig {
    public static String describe(net.minecraft.core.BlockPos pos) {
        return pos.toShortString();
    }
}
