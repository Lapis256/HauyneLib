package dev.lapis256.hauyne.kotlincheck;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageLoader;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.ForgeFeature;

public final class KotlinFeatureProvider implements IModLanguageLoader {
    public KotlinFeatureProvider() {
        var version = KotlinRuntime.detect(getClass().getClassLoader());
        if (version != null) {
            ForgeFeature.registerFeature(KotlinRuntime.FEATURE, new ForgeFeature.VersionFeatureTest(IModInfo.DependencySide.BOTH, version));
            ForgeFeature.registerFeature(KotlinRuntime.PRESENCE_FEATURE, new ForgeFeature.BooleanFeatureTest(IModInfo.DependencySide.BOTH, true));
        }
    }

    @Override
    public String name() {
        return "hauyne-kotlin-check";
    }

    @Override
    public String version() {
        return "1";
    }

    @Override
    public ModContainer loadMod(IModInfo info, ModFileScanData scan, ModuleLayer layer) {
        throw new UnsupportedOperationException("Use javafml to load mods with HauyneLib.");
    }
}
