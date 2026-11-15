package dev.lapis256.hauyne.config.minecraft

import net.minecraft.core.BlockPos

class MinecraftConfig {
    companion object {
        @JvmStatic
        fun describe(pos: BlockPos): String = pos.toShortString()
    }
}
