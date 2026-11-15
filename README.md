# HauyneLib

HauyneLib は、NeoForge / Forge 向けのライブラリ Mod です。コンフィグ機能を利用する Mod の開発には、KSP と `hauyne-config-ksp` が必須です。

公開バージョンと配布ファイルは [Releases](https://github.com/Lapis256/HauyneLib/releases) を参照してください。

## Gradle の設定

`build.gradle.kts` の `repositories` に公開 Maven リポジトリを追加してください。

```kotlin
repositories {
    maven("https://maven.lapis256.dev")
}
```

NeoForge の MDG プロジェクトでは、Mod 本体を `implementation` に追加します。以下は Minecraft 26.1.2 向けの例です。`hauyneVersion` は利用するリリースのバージョンに置き換えてください。

```kotlin
val hauyneVersion = "0.1.0"
val hauyneMinecraftVersion = "26.1-$hauyneVersion"

dependencies {
    implementation("dev.lapis256:hauyne-lib-neoforge:$hauyneMinecraftVersion")
}
```

コンフィグ機能を利用する場合は、KSP プラグインを適用し、同じ `build.gradle.kts` に次の依存関係も追加してください。`<KSP version>` は利用する KSP のバージョンに置き換えてください。コンフィグの実行時ライブラリは Mod 本体に同梱されています。

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<KSP version>"
}

dependencies {
    compileOnly("dev.lapis256:hauyne-config-neoforge:$hauyneMinecraftVersion")
    ksp("dev.lapis256:hauyne-config-ksp:$hauyneVersion")
}
```

`plugins` ブロックはファイル先頭の既存ブロックに追記してください。Minecraft 1.21.1 では、`hauyneMinecraftVersion` の接頭辞を `21.1-` に変更します。

## サポート対象

| Minecraft | ローダー |
| --- | --- |
| 26.1.2 | NeoForge |
| 1.21.1 | NeoForge |
| 1.20.1 | Forge |
