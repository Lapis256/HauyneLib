package dev.lapis256.hauyne.lib.loading;

import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.locating.ForgeFeature;

final class KotlinCheckLoadingErrors {
    static void report() {
        for (var mod : FMLLoader.getLoadingModList().getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if ("hauyneKotlinVersion".equals(bound.featureName()) && !ForgeFeature.testFeature(FMLEnvironment.dist, bound)) {
                    var message = "%s requires Kotlin %s (detected: %s). Install a compatible KotlinLangForge release.".formatted(mod.getModId(),
                            bound.featureBound(), ForgeFeature.featureValue(bound));
                    ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.technical_error", message).withAffectedMod(mod));
                }
            }
        }
    }
}
