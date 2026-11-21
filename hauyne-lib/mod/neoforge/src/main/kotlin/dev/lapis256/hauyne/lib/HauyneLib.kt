package dev.lapis256.hauyne.lib

import dev.lapis256.hauyne.config.neoforge.NeoforgeBackend
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent

@Mod(HauyneConstants.MOD_ID)
class HauyneLib(bus: IEventBus, container: ModContainer) {
    init {
        val backend = NeoforgeBackend(mapOf("welcome" to "HauyneLib"))
        container.registerConfig(ModConfig.Type.COMMON, backend.spec)
        bus.addListener { event: ModConfigEvent.Loading ->
            if (event.config.spec === backend.spec) backend.read("welcome")
        }
        bus.addListener { event: ModConfigEvent.Reloading ->
            if (event.config.spec === backend.spec) backend.read("welcome")
        }
    }
}
