package dev.lapis256.hauyne.kotlincheck;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

final class KotlinRuntime {
    static final String FEATURE = "hauyneKotlinVersion";
    static final String DEPENDENCY_NAME = "Kotlin standard library";
    static final String INSTALLATION_GUIDANCE = "Install a mod that provides a compatible Kotlin standard library, such as KotlinLangForge (recommended).";
    static final DefaultArtifactVersion NOT_INSTALLED = new DefaultArtifactVersion("null");

    private KotlinRuntime() {
    }

    static VersionRange dependencyRange(String required) {
        try {
            return VersionRange.createFromVersionSpec(required);
        } catch (InvalidVersionSpecificationException e) {
            return VersionRange.createFromVersion(required);
        }
    }

    static DefaultArtifactVersion detect(ClassLoader loader) {
        try {
            var kotlinVersion = Class.forName("kotlin.KotlinVersion", false, loader);
            var version = kotlinVersion.getField("CURRENT").get(null).toString();
            return new DefaultArtifactVersion(version);
        } catch (ClassNotFoundException e) {
            System.getLogger(KotlinRuntime.class.getName()).log(System.Logger.Level.WARNING,
                    "Kotlin standard library is missing.\n" + INSTALLATION_GUIDANCE);
            return null;
        } catch (LinkageError | ReflectiveOperationException e) {
            System.getLogger(KotlinRuntime.class.getName()).log(System.Logger.Level.WARNING, "Unable to read the Kotlin standard library version.",
                    e);
            return null;
        }
    }
}
