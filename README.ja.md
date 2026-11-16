# HauyneLib

[English](README.md)

HauyneLib は、NeoForge / Forge 向けのライブラリ Mod です。コンフィグ機能を利用する Mod の開発には、KSP と `hauyne-config-ksp` が必須です。

公開バージョンと配布ファイルは [Releases](https://github.com/Lapis256/HauyneLib/releases) を参照してください。

## Gradle の設定

`build.gradle.kts` の `repositories` に公開 Maven リポジトリを追加してください。

```kotlin
repositories {
    maven("https://maven.lapis256.dev")
}
```

NeoForge の MDG プロジェクトでは、Mod 本体を `implementation` に追加します。以下は Minecraft 26.1.2 向けの例です。`<version>` は利用するリリースのバージョンに置き換えてください。

```kotlin
dependencies {
    implementation("dev.lapis256:hauyne-lib-neoforge:26.1-<version>")
}
```

コンフィグ機能を利用する場合は、KSP プラグインを適用し、次の依存関係も追加してください。

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<KSP version>"
}

dependencies {
    ksp("dev.lapis256:hauyne-config-ksp:<version>")
}
```

## サポート対象

| Minecraft | ローダー |
| --- | --- |
| 26.1.2 | NeoForge |
| 1.21.1 | NeoForge |
| 1.20.1 | Forge |
