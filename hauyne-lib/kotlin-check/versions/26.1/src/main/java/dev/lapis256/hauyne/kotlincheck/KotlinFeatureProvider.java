package dev.lapis256.hauyne.kotlincheck;

import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.ForgeFeature;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;

public final class KotlinFeatureProvider implements ClassProcessorProvider {
    @Override
    public void createProcessors(Context context, Collector collector) {
        var loader = FMLLoader.getCurrent();
        var version = KotlinRuntime.detect(getClass().getClassLoader());
        var feature = version == null ? null : new ForgeFeature.VersionFeatureTest(IModInfo.DependencySide.BOTH, version);
        if (feature != null) {
            ForgeFeature.registerFeature(KotlinRuntime.FEATURE, feature);
        }

        for (var mod : loader.getLoadingModList().getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if (KotlinRuntime.FEATURE.equals(bound.featureName())
                        && (feature == null || !feature.test(feature.convertFromString(bound.featureBound())))) {
                    var detected = version == null ? "not installed or unavailable" : version.toString();
                    var message = "%s requires Kotlin %s (detected: %s). Install a compatible KotlinLangForge release.".formatted(mod.getModId(),
                            bound.featureBound(), detected);
                    ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.technical_error", message).withAffectedMod(mod));
                }
            }
        }
    }
}
