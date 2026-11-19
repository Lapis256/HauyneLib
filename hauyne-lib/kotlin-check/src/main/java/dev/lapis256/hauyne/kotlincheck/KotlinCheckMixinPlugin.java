package dev.lapis256.hauyne.kotlincheck;

import java.util.List;
import java.util.Set;

//? if <1.20.5 {
/*
import net.minecraftforge.fml.loading.EarlyLoadingException;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.locating.ForgeFeature;
*/
//?} else {
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.locating.ForgeFeature;
//?}
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class KotlinCheckMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        // 言語ローダーの初期化時点では Mod 一覧が未確定なので、ここで要求元を調べる。
        var loading = FMLLoader.getLoadingModList();
        for (var mod : loading.getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if (KotlinRuntime.FEATURE.equals(bound.featureName()) && !ForgeFeature.testFeature(FMLEnvironment.dist, bound)) {
                    var message = "%s requires Kotlin %s (detected: %s). Install a compatible KotlinLangForge release.".formatted(mod.getModId(),
                            bound.featureBound(), ForgeFeature.featureValue(bound));
                    // @formatter:off
                    //? if <1.20.5 {
                    /*
                    var error = new EarlyLoadingException.ExceptionData("Kotlin runtime check failed: {3}", mod, message);
                    loading.getErrors().add(new EarlyLoadingException(message, null, List.of(error)));
                    */
                    //?} else {
                    ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.technical_error", message).withAffectedMod(mod));
                    //?}
                    // @formatter:on
                }
            }
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
