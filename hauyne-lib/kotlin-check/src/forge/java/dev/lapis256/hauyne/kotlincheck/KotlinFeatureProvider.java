package dev.lapis256.hauyne.kotlincheck;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.forgespi.language.ILifecycleEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.IModLanguageProvider;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.locating.ForgeFeature;

public final class KotlinFeatureProvider implements IModLanguageProvider {
    // Forge は言語プロバイダーを再列挙するため、機能登録はクラス初期化時に一度だけ行う。
    static {
        var version = KotlinRuntime.detect(KotlinFeatureProvider.class.getClassLoader());
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
    public Consumer<ModFileScanData> getFileVisitor() {
        return scan -> {
        };
    }

    @Override
    public <R extends ILifecycleEvent<R>> void consumeLifecycleEvent(Supplier<R> event) {
    }
}
