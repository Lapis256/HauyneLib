package dev.lapis256.hauyne.kotlincheck;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

final class KotlinRuntime {
    static final String FEATURE = "hauyneKotlinVersion";
    private static final String INSTALLATION_GUIDANCE = "Install a mod that provides a compatible Kotlin standard library, such as KotlinLangForge (recommended).";

    private KotlinRuntime() {
    }

    static String requirementMessage(String modId, String required, Object detected, boolean styled) {
        var template = """
                §e%s§r requires the §6Kotlin standard library§r.
                Required version: §o%s§r
                §7Detected version: §o%s§r

                %s""";
        return (styled ? template : template.replaceAll("§[67eor]", "")).formatted(modId, required, detected, INSTALLATION_GUIDANCE);
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
