# ビルドと公開

## 構成

Gradle 9.7.1 と Java 25 で実行します。Stonecutter 0.10 は Gradle 9.7 以上を要求するため、指示書の候補 9.2.1 から変更しました。Toolchain は Minecraft 26.1.2 が Java 25、1.21.1 が Java 21、Forge 1.20.1 と JVM 共通ライブラリが Java 17 です。

`buildSrc` は使いません。共通ビルド処理はすべて `build-logic` の Convention Plugin にまとめています。ルートの `allprojects`・`subprojects` による横断設定や、`gradle/*.gradle.kts` の外部スクリプト適用はありません。MDG のモード、依存先、Jar-in-Jar、配布ファイルは各モジュールの `build.gradle.kts` に記述しています。

共通処理へ渡す値は型付き Extension で設定します。`hauyneLibrary.componentPath` で成果物名の生成元、`hauyneMinecraft` で Minecraft バージョンと設定ファイル、`hauyneModMetadata` でローダー・パッケージ・依存範囲などを指定します。`extra` やプロパティの有無からローダーを推測する処理はありません。必須のローダー指定を省くと構成時に失敗します。

```kotlin
hauyneLibrary { componentPath = parent!!.path }
hauyneMinecraft {
    minecraftVersion = stonecutter.current.version
    versionPropertiesFile = rootProject.layout.projectDirectory.file("versions/${stonecutter.current.version}/gradle.properties")
}
```

Mod メタデータでは `loader = HauyneModLoader.NEOFORGE` または `FORGE` を明示します。`loaderVersionRange` と `javaFmlLoaderVersionRange` はバージョン別設定から渡します。これらの値は、移行前の生成結果を維持して設定ファイルへ移しています。MDG 固有のバージョンは `hauyneMinecraft.versionProperty(...)` から読み、既存の MDG Extension に渡します。

モジュールのルートは `hauyne-config/` と `hauyne-lib/`、Gradle パスは `:hauyne-config:*` と `:hauyne-lib:*` です。Minecraft 共通層は `:hauyne-config:minecraft` のままです。`hauyne-lib:common` は作成していません。将来追加する場合のパッケージは `dev.lapis256.hauyne.lib` とします。

公開 API は `dev.lapis256.hauyne.config`、実装は `dev.lapis256.hauyne.config.<module>` に置きます。Mod 実装は `dev.lapis256.hauyne.lib.neoforge` と `dev.lapis256.hauyne.lib.forge` に分けています。KSP のサービス登録先と生成ソースも、この構成に合わせています。

依存バージョンは `gradle/libs.versions.toml`、リリース情報はルートの `gradle.properties` で変更してください。Minecraft ごとの Java・NeoForm・NeoForge・Forge の値は `versions/<Minecraft>/gradle.properties` に置き、同じゲーム版の全モジュールで共有します。Stonecutter と Toolchain resolver の設定プラグインは初期化順の都合で `settings.gradle.kts` に固定しています。

Java 17 の制約は共通 JVM ライブラリに適用します。Minecraft 側の Toolchain 指定は各モジュールの `java` ブロックから、対応するバージョンファイルの `javaVersion` を参照します。Kotlin の共通設定で Minecraft 側を Java 17 に固定することはありません。

例えば `versions/26.1.2/gradle.properties` は次の内容です。各モジュールの Stonecutter 生成ディレクトリへ同じ値を複製する必要はありません。

```properties
javaVersion=25
neoFormVersion=26.1.2-1
neoForgeVersion=26.1.2.126
loaderVersionRange=[26.1.2.126,)
javaFmlLoaderVersionRange=[1,)
```

| 対象 | Java / JVM target |
| --- | --- |
| `hauyne-config:core`・`hauyne-config:ksp`・`hauyne-config:memory`・`hauyne-config:nightconfig` | 17 |
| `examples:jvm` | 21 |
| Minecraft 26.1.2 の共通層・NeoForge アダプター・Mod | 25 |
| Minecraft 1.21.1 の共通層・NeoForge アダプター・Mod | 21 |
| Minecraft 1.20.1 の共通層・Forge アダプター・Mod | 17 |

Minecraft の共通層は NeoForm Vanilla、1.20.1 のみ LegacyForge MCP Vanilla です。アダプターはローダーごとに分けています。現在の Minecraft バージョンは `stonecutter.current.version`、バージョンの比較は `stonecutter.eval(...)` で扱います。プロジェクト名を Minecraft バージョンとみなす処理は使いません。

Java の Mod 初期化クラスを使い、Core・対応する Minecraft 共通層・アダプターを Jar-in-Jar に含めます。Kotlin ランタイムと KSP は同梱しません。開発用に必要な Kotlin クラスパスと、本番 Mod への同梱は別の設定です。本番の Kotlin ランタイム供給方法は未設定で、KotlinForForge などの必須依存も未登録です。ライブラリをロードする本番環境の動作確認は完了していません。

内部の Gradle group は同名のバージョンプロジェクト間の衝突を避けるため分けています。公開座標の groupId はすべて `dev.lapis256` です。artifactId はプロジェクトパスをハイフンでつないで生成し、Stonecutter のバージョンノードでは controller のパスを使います。例は `hauyne-config-core`、`hauyne-config-minecraft`、`hauyne-lib-neoforge` です。`hauyne-config:memory` は公開しません。

### バージョンの採番

`gradle.properties` の `modVersion` を全モジュール共通の `x.y.z` とします。MC に依存しないライブラリは `x.y.z`、MC に依存するライブラリと Mod 本体は `<MC 系列>-x.y.z` を使います。

| Minecraft | MC 系列 | modVersion が `0.1.0` のとき |
| --- | --- | --- |
| 1.20.1 | `20.1` | `20.1-0.1.0` |
| 1.21.1 | `21.1` | `21.1-0.1.0` |
| 26.1.2 | `26.1` | `26.1-0.1.0` |

同じ機能のリリースには MC 系列をまたいで同じ `x.y.z` を付けます。特定の系列だけを修正する場合は、その系列だけパッチ番号を進めて公開できます。モジュールごとの独立した採番は行いません。同じ `x.y.z` でも MC 系列間のバイナリ互換性は意味しません。Minecraft の依存範囲と Toolchain の指定には完全な Minecraft バージョンを使います。

## ローカル検証

Windows では `./gradlew` を `./gradlew.bat` に読み替えてください。

```sh
./gradlew help projects
./gradlew build test publishToMavenLocal
./gradlew verifyRequired -PmodPublishDryRun=true
./gradlew :hauyne-lib:forge:1.20.1:publishMods -PmodPublishDryRun=true
python scripts/verify_artifacts.py 26.1.2 1.21.1 1.20.1
```

`verifyRequired` は必須の NeoForge 2バージョンと JVM/KSP 例のビルド・テスト・ローカル公開・配布 dry-run を実行します。Forge は別に検証できます。

```sh
./gradlew :hauyne-config:minecraft:1.20.1:build :hauyne-config:forge:1.20.1:build :hauyne-lib:forge:1.20.1:build
./gradlew :hauyne-config:minecraft:1.20.1:publishToMavenLocal :hauyne-config:forge:1.20.1:publishToMavenLocal :hauyne-lib:forge:1.20.1:publishToMavenLocal
./gradlew :hauyne-lib:forge:1.20.1:runServer
```

Forge の配布ファイルには `reobfJar` の出力を使います。開発用 JAR は `build/devlibs`、配布用は `build/libs` です。Mixin と Access Transformer は使用していません。

Stonecutter の切替は関連する controller をまとめて行います。Forge を調べるときは Minecraft 共通層も切り替えてください。

```sh
./gradlew ':hauyne-config:minecraft:Set active project to 1.21.1' ':hauyne-config:neoforge:Set active project to 1.21.1' ':hauyne-lib:neoforge:Set active project to 1.21.1'
./gradlew :hauyne-lib:neoforge:1.21.1:build
./gradlew ':hauyne-config:minecraft:Reset active project' ':hauyne-config:neoforge:Reset active project' ':hauyne-lib:neoforge:Reset active project'
./gradlew ':hauyne-config:minecraft:Set active project to 1.20.1'
./gradlew :hauyne-lib:forge:1.20.1:build
./gradlew ':hauyne-config:minecraft:Reset active project'
```

既定アクティブバージョンは 26.1.2、Forge の controller は 1.20.1 です。各モジュール直下の `src/` は共有ソース、各モジュールの `versions/<Minecraft>/src/` はバージョン別ソースです。共有ソースと同じ相対パスにバージョン別ソースを置くと、そのバージョンの実装を上書きできます。バージョン別ソースは Git の管理対象です。Stonecutter の生成先は `build/generated/stonecutter/` で、通常の `build/` 除外に含まれます。

バージョン別設定の読み込みも既存機能を確認しています。Stonecutter 0.10 の構造化プロパティは JSON・TOML・YAML 用で、今回指定された `gradle.properties` は対象外です。そのため、ここだけは Java 標準の `Properties` と Gradle の `providers.fileContents` を使っています。独自のバージョン比較やファイル解析器は実装していません。

## 認証情報とプロジェクト ID

GitHub の `release` Environment に承認ルールを設定してください。リポジトリのファイルだけでは環境保護を有効化できません。

| 種類 | 設定名 | 用途 |
| --- | --- | --- |
| Secret | `R2_ACCESS_KEY` / `R2_SECRET_KEY` | R2 の Maven バケットへの書き込み |
| Variable | `R2_ENDPOINT` | R2 の S3 エンドポイント |
| Secret | `CURSEFORGE_TOKEN` | CurseForge の公開 |
| Secret | `MODRINTH_TOKEN` | Modrinth の公開 |
| Gradle property | `curseforgeProjectId` / `modrinthProjectId` | 実在する HauyneLib プロジェクトの ID |

プロジェクト ID は未設定です。実際の ID を確認して `gradle.properties` に記入してください。架空 ID は使っていません。`r2Bucket` の既定値は `maven` です。R2 側のバケットと公開ドメインの準備も必要です。

## 公開手順

通常のビルドやローカル公開には資格情報は不要です。Mod 公開は既定で dry-run です。本番公開には `-PmodPublishDryRun=false` が必要で、ID やトークンの不足はエラーになります。認証情報をコマンド引数やソースへ直接書かないでください。

以下は本番公開用です。今回のセットアップ作業では実行していません。

```sh
./gradlew :hauyne-config:core:publish :hauyne-config:ksp:publish :hauyne-config:nightconfig:publish "-Dorg.gradle.s3.endpoint=$R2_ENDPOINT"
./gradlew :hauyne-config:minecraft:26.1.2:publish :hauyne-config:neoforge:26.1.2:publish :hauyne-lib:neoforge:26.1.2:publish "-Dorg.gradle.s3.endpoint=$R2_ENDPOINT"
./gradlew :hauyne-lib:neoforge:26.1.2:publishMods -PmodPublishDryRun=false
```

1.21.1 も同じ手順で、プロジェクトパスの Minecraft バージョンを置き換えます。配布バージョンにはローダー名も加え、異なる成果物を区別します。公開チャンネルは `releaseChannel` の `ALPHA`・`BETA`・`STABLE` で指定します。

CI は3バージョンを独立した matrix job で検証します。Release workflow は `v<modVersion>` タグとの一致を確認し、必須2バージョンの検証後に、環境承認を経て Maven、CurseForge、Modrinth、GitHub Release 添付の順に処理します。Forge はゲーム起動の検証が完了するまで本番公開に含めません。

一部の公開先だけ失敗した場合は、公開済みのバージョンとファイルを各サービスで確認し、失敗した公開先のタスクだけを再実行してください。`publishCurseforge` と `publishModrinth` を個別に指定できます。公開済みバージョンへの再登録はサービス側で拒否される可能性があるため、公開ジョブ全体を無条件に再実行しないでください。

## 検証の範囲

ゲームサーバーの EULA 同意は自動設定しません。Forge の起動試行は EULA 確認までであり、Mod 初期化・ワールド起動・配布 JAR をインストールした本番環境での動作は未検証です。Forge の追加対応は未完了として扱います。

本番 R2 接続、CurseForge・Modrinth の実 ID と認証、GitHub Actions のホスト上での実行は未検証です。公開 API は最小サンプルであり、ライブラリ全機能の実装を意味しません。

参照した公式資料: [MDG](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)、[LegacyForge](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md)、[Mod Publish Plugin](https://modmuss50.github.io/mod-publish-plugin/options/)、[Gradle S3](https://docs.gradle.org/current/userguide/supported_repository_protocols.html)。
