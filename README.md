# HauyneLib

Gradle マルチプロジェクトの設定ライブラリと、NeoForge / Forge 用 Mod です。

| MC 系列 | Minecraft | ローダー | 本番 Release の対象 |
| --- | --- | --- | --- |
| `26.1` | 26.1.2 | NeoForge | 対象 |
| `21.1` | 1.21.1 | NeoForge | 対象 |
| `20.1` | 1.20.1 | Forge | 対象 |

各モジュールの対応対象は [settings.gradle.kts](settings.gradle.kts) の Stonecutter 定義にあります。設定は各モジュール内に置きます。例えば共通層は `hauyne-config/minecraft/versions/21.1/`、NeoForge 用 Mod は `hauyne-lib/neoforge/versions/21.1/` です。

## JVM ライブラリを使う

公開 Maven リポジトリを利用側の `repositories` に登録し、次の依存関係を追加します。

```kotlin
dependencies {
    implementation("dev.lapis256:hauyne-config-nightconfig:0.1.0")
}
```

```kotlin
import dev.lapis256.hauyne.config.nightconfig.TomlBackend
import java.nio.file.Path

TomlBackend(Path.of("config.toml")).use { backend ->
    backend.write("welcome", "HauyneLib")
    println(backend.read("welcome"))
}
```

共通 API の座標は `dev.lapis256:hauyne-config-core:0.1.0`、KSP は `dev.lapis256:hauyne-config-ksp:0.1.0` です。Minecraft 用の成果物は `<MC 系列>-<modVersion>` を使い、例えば `dev.lapis256:hauyne-config-neoforge:26.1-0.1.0` です。本番 Maven リポジトリの公開 URL はまだ案内していません。

## 開発する

Java 25 で `./gradlew :build-logic:test check` を実行すると、ビルド処理のテスト、JVM と Minecraft 26.1.2、1.21.1、1.20.1 のテスト、整形チェックを実行できます。公開可能かの確認は、GitHub Actions の Release workflow を `dry_run=true` で実行してください。Windows では `./gradlew.bat` を使ってください。

```sh
./gradlew ":hauyne-lib:neoforge:Set active project to 21.1"
./gradlew runActiveServer
./gradlew resetActiveProjects
./gradlew checkActiveProjects
```

切替は controller ごとに行います。依存先のターゲットは明示されているため、他の controller を揃える必要はありません。`runActiveServer` は既定で lib の NeoForge controller を使います。Forge の場合は `-PserverController=:hauyne-lib:forge` を指定します。リリース用の成果物は `.release/assets/` に収集します。

構成、Forge 1.20.1 の追加検証、認証情報、公開先ごとの再実行手順は [ビルドと公開](docs/publishing.md) を参照してください。公開済みの成果物を確認し、失敗した公開先のタスクだけを再実行してください。

通常の Mod 公開タスクは dry-run が既定です。
