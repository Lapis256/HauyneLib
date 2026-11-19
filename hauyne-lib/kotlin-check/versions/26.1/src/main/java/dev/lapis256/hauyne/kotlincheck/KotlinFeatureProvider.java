package dev.lapis256.hauyne.kotlincheck;

import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.ForgeFeature;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;

public final class KotlinFeatureProvider implements ClassProcessorProvider {
    @Override
    public void createProcessors(Context context, Collector collector) {
        var version = KotlinRuntime.detect(getClass().getClassLoader());
        if (version != null) {
            ForgeFeature.registerFeature(KotlinRuntime.FEATURE, new ForgeFeature.VersionFeatureTest(IModInfo.DependencySide.BOTH, version));
            ForgeFeature.registerFeature(KotlinRuntime.PRESENCE_FEATURE, new ForgeFeature.BooleanFeatureTest(IModInfo.DependencySide.BOTH, true));
        }
    }
}
