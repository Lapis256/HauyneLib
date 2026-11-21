package dev.lapis256.hauyne.lib

import dev.lapis256.hauyne.config.forge.ForgeBackend
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.config.ModConfigEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext

@Mod(HauyneConstants.MOD_ID)
class HauyneLib {
    init {
        val backend = ForgeBackend(mapOf("welcome" to "HauyneLib"))
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, backend.spec)
        val bus = FMLJavaModLoadingContext.get().modEventBus
        bus.addListener { event: ModConfigEvent.Loading ->
            if (event.config.getSpec<ForgeConfigSpec>() === backend.spec) backend.read("welcome")
        }
        bus.addListener { event: ModConfigEvent.Reloading ->
            if (event.config.getSpec<ForgeConfigSpec>() === backend.spec) backend.read("welcome")
        }
    }
}
