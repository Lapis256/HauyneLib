package dev.lapis256.hauyne.lib.loading;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.fml.IModLoadingState;
import net.minecraftforge.fml.IModStateProvider;
import net.minecraftforge.fml.LoadingFailedException;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.locating.ForgeFeature;

public final class KotlinCheckStateProvider implements IModStateProvider {
    @Override
    public List<IModLoadingState> getAllStates() {
        // 状態の実行より前に Forge が features を検査するので、状態を列挙する時点で失敗を伝える。
        var errors = new ArrayList<ModLoadingException>();
        for (var mod : FMLLoader.getLoadingModList().getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if ("hauyneKotlinVersion".equals(bound.featureName()) && !ForgeFeature.testFeature(FMLEnvironment.dist, bound)) {
                    var message = "%s requires Kotlin %s (detected: %s). Install a compatible KotlinLangForge release.".formatted(mod.getModId(),
                            bound.featureBound(), ForgeFeature.featureValue(bound));
                    errors.add(new ModLoadingException(mod, ModLoadingStage.VALIDATE, "Kotlin runtime check failed: {3}", null, message));
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new LoadingFailedException(errors);
        }
        return List.of();
    }
}
