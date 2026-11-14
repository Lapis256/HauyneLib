package dev.lapis256.hauyne.lib.forge;

import net.minecraftforge.fml.common.Mod;

@Mod(HauyneConstants.MOD_ID)
public final class HauyneLib {

    public HauyneLib() {
        var backend = new dev.lapis256.hauyne.config.forge.ForgeBackend(java.util.Map.of("welcome", "HauyneLib"));
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, backend.spec);
        var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Loading event) -> {
            if (event.getConfig().getSpec() == backend.spec)
                backend.read("welcome");
        });
        bus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Reloading event) -> {
            if (event.getConfig().getSpec() == backend.spec)
                backend.read("welcome");
        });
    }
}
