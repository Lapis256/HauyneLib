# HauyneLib

Gradle マルチプロジェクトの設定ライブラリと、NeoForge / Forge 用 Mod です。

| MC 系列 | Minecraft | ローダー | 本番 Release の対象 |
| --- | --- | --- | --- |
| `26.1` | 26.1.2 | NeoForge | 対象 |
| `21.1` | 1.21.1 | NeoForge | 対象 |
| `20.1` | 1.20.1 | Forge | 起動検証が未完了のため対象外 |

対応対象の宣言は [versions/targets.properties](versions/targets.properties) にあります。

## JVM ライブラリを使う

ローカルでビルドした成果物を使う例です。HauyneLib 側で `./gradlew prepareJvmPublications` を実行してから、利用側の Gradle に次の設定を追加します。

```kotlin
repositories {
    mavenCentral()
    mavenLocal()
}

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

Java 25 で `./gradlew verifyRequired -PmodPublishDryRun=true` を実行すると、Minecraft 26.1.2 と 1.21.1 のビルド・テスト・ローカル Maven 公開・Mod 配布 dry-run を検証できます。Windows では `./gradlew.bat` を使ってください。

```sh
./gradlew switchTo21.1
./gradlew runActiveServer
./gradlew resetActiveProjects
./gradlew checkActiveProjects
./gradlew buildAndCollect
```

切替は Minecraft 共通層・アダプター・Mod をまとめて行います。収集した配布 JAR と sources JAR は `build/collected/` に出力します。

構成、Forge 1.20.1 の追加検証、認証情報、公開先ごとの再実行手順は [ビルドと公開](docs/publishing.md) を参照してください。公開済みの成果物を確認し、失敗した公開先のタスクだけを再実行してください。

Forge はゲーム起動完了まで未検証のため本番リリースには含めません。通常の Mod 公開タスクは dry-run が既定です。
