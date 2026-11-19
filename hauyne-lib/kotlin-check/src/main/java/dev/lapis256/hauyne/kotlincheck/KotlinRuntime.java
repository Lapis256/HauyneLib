package dev.lapis256.hauyne.kotlincheck;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

final class KotlinRuntime {
    static final String FEATURE = "hauyneKotlinVersion";
    static final String PRESENCE_FEATURE = "Kotlin runtime (install a compatible KotlinLangForge release)";

    private KotlinRuntime() {
    }

    static DefaultArtifactVersion detect(ClassLoader loader) {
        try {
            var kotlinVersion = Class.forName("kotlin.KotlinVersion", false, loader);
            var version = kotlinVersion.getField("CURRENT").get(null).toString();
            return new DefaultArtifactVersion(version) {
                @Override
                public String toString() {
                    return version + " (install a compatible KotlinLangForge release)";
                }
            };
        } catch (ClassNotFoundException e) {
            return null;
        } catch (LinkageError | ReflectiveOperationException e) {
            System.getLogger(KotlinRuntime.class.getName()).log(System.Logger.Level.WARNING, "Unable to read the Kotlin standard library version.",
                    e);
            return null;
        }
    }
}
