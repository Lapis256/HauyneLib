package dev.lapis256.hauyne.kotlincheck;

import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.ForgeFeature;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;

public final class KotlinFeatureProvider implements ClassProcessorProvider {
    @Override
    public void createProcessors(Context context, Collector collector) {
        var loader = FMLLoader.getCurrent();
        var version = KotlinRuntime.detect(getClass().getClassLoader());
        if (version != null) {
            ForgeFeature.registerFeature(KotlinRuntime.FEATURE, new ForgeFeature.VersionFeatureTest(IModInfo.DependencySide.BOTH, version));
        }

        for (var mod : loader.getLoadingModList().getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if (KotlinRuntime.FEATURE.equals(bound.featureName()) && !ForgeFeature.testFeature(loader.getDist(), bound)) {
                    var detected = version == null ? "not installed or unavailable" : version.toString();
                    var styled = loader.getDist().isClient() && FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL);
                    var message = KotlinRuntime.requirementMessage(mod.getModId(), bound.featureBound(), detected, styled);
                    ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.technical_error", message).withAffectedMod(mod));
                }
            }
        }
    }
}
