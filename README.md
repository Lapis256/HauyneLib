# HauyneLib

[日本語](README.ja.md)

HauyneLib is a library mod for NeoForge / Forge. Developing mods that use its configuration features requires KSP and `hauyne-config-ksp`.

See [Releases](https://github.com/Lapis256/HauyneLib/releases) for published versions and distribution files.

## Gradle setup

Add the public Maven repository to `repositories` in your `build.gradle.kts`.

```kotlin
repositories {
    maven("https://maven.lapis256.dev")
}
```

In a NeoForge MDG project, add the mod to `implementation`. The following example targets Minecraft 26.1.2. Replace `<version>` with the release version you want to use.

```kotlin
dependencies {
    implementation("dev.lapis256:hauyne-lib-neoforge:26.1-<version>")
}
```

To use the configuration features, apply the KSP plugin and add the following dependency as well.

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<KSP version>"
}

dependencies {
    ksp("dev.lapis256:hauyne-config-ksp:<version>")
}
```

## Kotlin runtime requirement

HauyneLib checks the Kotlin standard library before mod initialization. Install a compatible KotlinLangForge release to provide the runtime. A mod can declare its own requirement in its `neoforge.mods.toml` or `mods.toml`, using its mod ID and a Maven version range.

```toml
[features.examplemod]
hauyneKotlinVersion = "[2.4.21,)"
```

## Supported versions

| Minecraft | Loader |
| --- | --- |
| 26.1.2 | NeoForge |
| 1.21.1 | NeoForge |
| 1.20.1 | Forge |
