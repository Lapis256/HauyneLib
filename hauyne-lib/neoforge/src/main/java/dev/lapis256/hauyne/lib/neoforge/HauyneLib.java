package dev.lapis256.hauyne.lib.neoforge;

import net.neoforged.fml.common.Mod;

@Mod(HauyneConstants.MOD_ID)
public final class HauyneLib {

    public HauyneLib(net.neoforged.bus.api.IEventBus bus, net.neoforged.fml.ModContainer container) {
        var backend = new dev.lapis256.hauyne.config.neoforge.NeoforgeBackend(java.util.Map.of("welcome", "HauyneLib"));
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, backend.spec);
        bus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading event) -> {
            if (event.getConfig().getSpec() == backend.spec)
                backend.read("welcome");
        });
        bus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading event) -> {
            if (event.getConfig().getSpec() == backend.spec)
                backend.read("welcome");
        });
    }
}
