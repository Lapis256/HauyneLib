package dev.lapis256.hauyne.kotlincheck;

import java.util.Optional;

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
        if (version != null) {
            ForgeFeature.registerFeature(KotlinRuntime.FEATURE, new ForgeFeature.VersionFeatureTest(IModInfo.DependencySide.BOTH, version));
        }

        for (var mod : loader.getLoadingModList().getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if (KotlinRuntime.FEATURE.equals(bound.featureName()) && !ForgeFeature.testFeature(loader.getDist(), bound)) {
                    var required = KotlinRuntime.dependencyRange(bound.featureBound());
                    var detected = version == null ? KotlinRuntime.NOT_INSTALLED : version;
                    var issue = ModLoadingIssue.error("fml.modloadingissue.missingdependency", KotlinRuntime.DEPENDENCY_NAME, mod.getModId(),
                            required, detected, Optional.of(KotlinRuntime.INSTALLATION_GUIDANCE));
                    ModLoader.addLoadingIssue(issue.withAffectedMod(mod));
                }
            }
        }
    }
}
