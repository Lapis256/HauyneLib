package dev.lapis256.hauyne.kotlincheck;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

final class KotlinRuntime {
    static final String FEATURE = "hauyneKotlinVersion";

    private KotlinRuntime() {
    }

    static DefaultArtifactVersion detect(ClassLoader loader) {
        try {
            var kotlinVersion = Class.forName("kotlin.KotlinVersion", false, loader);
            var version = kotlinVersion.getField("CURRENT").get(null).toString();
            return new DefaultArtifactVersion(version);
        } catch (ClassNotFoundException e) {
            System.getLogger(KotlinRuntime.class.getName()).log(System.Logger.Level.WARNING,
                    "Kotlin standard library is missing. Install a compatible KotlinLangForge release.");
            return null;
        } catch (LinkageError | ReflectiveOperationException e) {
            System.getLogger(KotlinRuntime.class.getName()).log(System.Logger.Level.WARNING, "Unable to read the Kotlin standard library version.",
                    e);
            return null;
        }
    }
}
