package dev.lapis256.hauyne.kotlincheck;

import java.util.List;
import java.util.Optional;
import java.util.Set;

//? if <1.20.5 {
/*
import net.minecraftforge.fml.loading.EarlyLoadingException;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.locating.ForgeFeature;
import net.minecraftforge.forgespi.language.IModInfo;
*/
//?} else {
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.locating.ForgeFeature;
import net.neoforged.neoforgespi.language.IModInfo;
//?}
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class KotlinCheckMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        var version = KotlinRuntime.detect(KotlinCheckMixinPlugin.class.getClassLoader());
        if (version != null) {
            ForgeFeature.registerFeature(KotlinRuntime.FEATURE, new ForgeFeature.VersionFeatureTest(IModInfo.DependencySide.BOTH, version));
        }

        var loading = FMLLoader.getLoadingModList();
        for (var mod : loading.getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if (KotlinRuntime.FEATURE.equals(bound.featureName()) && !ForgeFeature.testFeature(FMLEnvironment.dist, bound)) {
                    var required = KotlinRuntime.dependencyRange(bound.featureBound());
                    var detected = version == null ? KotlinRuntime.NOT_INSTALLED : version;
                    // @formatter:off
                    //? if <1.20.5 {
                    /*
                    var error = new EarlyLoadingException.ExceptionData("fml.modloading.missingdependency", mod,
                            KotlinRuntime.DEPENDENCY_NAME, mod.getModId(), required, detected);
                    loading.getErrors().add(new EarlyLoadingException(KotlinRuntime.INSTALLATION_GUIDANCE, null, List.of(error)));
                    */
                    //?} else {
                    ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.missingdependency",
                            KotlinRuntime.DEPENDENCY_NAME, mod.getModId(), required, detected,
                            Optional.of(KotlinRuntime.INSTALLATION_GUIDANCE)).withAffectedMod(mod));
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
