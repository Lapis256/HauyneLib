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

## サポート対象

| Minecraft | ローダー |
| --- | --- |
| 26.1.2 | NeoForge |
| 1.21.1 | NeoForge |
| 1.20.1 | Forge |
