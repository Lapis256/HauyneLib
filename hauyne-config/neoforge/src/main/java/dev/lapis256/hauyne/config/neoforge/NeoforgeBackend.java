package dev.lapis256.hauyne.config.neoforge;

import dev.lapis256.hauyne.config.Backend;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.Map;
import java.util.HashMap;

public final class NeoforgeBackend implements Backend {
    private final Map<String, ModConfigSpec.ConfigValue<String>> values = new HashMap<>();
    public final ModConfigSpec spec;

    public NeoforgeBackend(Map<String, String> defaults) {
        var builder = new ModConfigSpec.Builder();
        defaults.forEach((key, value) -> values.put(key, builder.define(key, value)));
        spec = builder.build();
    }

    public String read(String key) {
        var value = values.get(key);
        return value == null ? null : value.get();
    }

    public void write(String key, String value) {
        var entry = values.get(key);
        if (entry == null)
            throw new IllegalArgumentException("Unknown configuration key: " + key);
        entry.set(value);
        spec.save();
    }
}
