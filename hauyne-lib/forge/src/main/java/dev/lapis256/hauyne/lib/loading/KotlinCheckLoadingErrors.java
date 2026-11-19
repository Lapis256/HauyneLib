package dev.lapis256.hauyne.lib.loading;

import java.util.List;

import net.minecraftforge.fml.loading.EarlyLoadingException;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.locating.ForgeFeature;

final class KotlinCheckLoadingErrors {
    static void report() {
        var loading = FMLLoader.getLoadingModList();
        for (var mod : loading.getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if ("hauyneKotlinVersion".equals(bound.featureName()) && !ForgeFeature.testFeature(FMLEnvironment.dist, bound)) {
                    var message = "%s requires Kotlin %s (detected: %s). Install a compatible KotlinLangForge release.".formatted(mod.getModId(),
                            bound.featureBound(), ForgeFeature.featureValue(bound));
                    var error = new EarlyLoadingException.ExceptionData("Kotlin runtime check failed: {3}", mod, message);
                    loading.getErrors().add(new EarlyLoadingException(message, null, List.of(error)));
                }
            }
        }
    }
}
