# ビルドと公開

## 構成

Gradle 9.7.1 と Java 25 で実行します。Stonecutter 0.10 は Gradle 9.7 以上を要求するため、指示書の候補 9.2.1 から変更しました。Toolchain は Minecraft 26.1.2 が Java 25、1.21.1 が Java 21、Forge 1.20.1 と JVM 共通ライブラリが Java 17 です。

`buildSrc` は使いません。共通ビルド処理はすべて `build-logic` の Convention Plugin にまとめています。ルートの `allprojects`・`subprojects` による横断設定や、`gradle/*.gradle.kts` の外部スクリプト適用はありません。MDG のモード、依存先、Jar-in-Jar、配布ファイルは各モジュールの `build.gradle.kts` に記述しています。

`build-logic/src/main/kotlin/dev/lapis256/hauyne/buildlogic/` は機能別に分けています。`library` はライブラリの共通設定、`minecraft` はゲーム系列・Toolchain・Mod の設定と配布、`metadata` は Mod 情報の型と生成タスク、`publishing` は Maven 公開、`release` はバージョン更新と成果物の記録、`formatting` は整形設定です。テストも対応する機能の package に置きます。プラグインスクリプトには package 宣言を付けず、ファイル名をプラグイン ID に使います。独自の Maven 公開プラグインは、Gradle 標準の `maven-publish` と区別するため `maven-publishing` とします。

通常のライブラリには `library` を適用し、Kotlin・Java・テスト・JAR の共通設定を有効にします。Maven 公開が必要なモジュールには、加えて `maven-publishing` を適用します。Minecraft 用の `minecraft-library` は `library` と Maven 公開の設定をまとめて適用します。

共通処理へ渡す値は型付き Extension で設定します。Minecraft モジュールでは `minecraft-library` が controller のパスから成果物名を設定し、系列に対応する Minecraft バージョン・設定ファイル・Java Toolchain の既定値も設定します。各モジュールで同じ初期設定を繰り返す必要はありません。`hauyneLibrary` と `hauyneMinecraft` の Extension は引き続き使用できます。

Mod 共通の ID・表示名・パッケージ・ライセンス・説明は、ルートの `gradle.properties` の `modId`・`modName`・`modPackage`・`license`・`modDescription` に設定します。`mod` プラグインが `hauyneModMetadata` の既定値として読み込みます。Forge と NeoForge の Mod には `mod` を適用し、`hauyneFmlMetadata` に `loader = HauyneFmlLoader.NEOFORGE` または `FORGE` と依存範囲を指定します。必須のローダー指定を省くと構成時に失敗します。`loaderVersionRange` と `javaFmlLoaderVersionRange` はバージョン別設定から渡します。MDG 固有のバージョンは `hauyneMinecraft.versionProperty(...)` から読み、既存の MDG Extension に渡します。

`generateModMetadata` が Mod の公開バージョンと設定値から TOML を生成し、`processResources` が JAR の `META-INF/` に取り込みます。Forge は `mods.toml`、NeoForge は `neoforge.mods.toml` を生成します。テンプレートファイルは不要です。メタデータは Kotlin Serialization の `@Serializable` クラスで定義し、[ktoml](https://github.com/orchestr7/ktoml) で TOML にシリアライズします。出力項目は [Forge](https://docs.minecraftforge.net/en/latest/gettingstarted/modfiles/) と [NeoForge](https://docs.neoforged.net/docs/gettingstarted/modfiles/) の仕様に従います。

`mod` は `minecraft-library` を適用し、定数・FML メタデータの生成、Jar-in-Jar の依存設定、CurseForge・Modrinth への配布設定をまとめます。配布 JAR とローダー固有の設定は各 Mod の `build.gradle.kts` で指定します。現時点では Fabric のビルド・起動には対応していません。

モジュールのルートは `hauyne-config/` と `hauyne-lib/`、Gradle パスは `:hauyne-config:*` と `:hauyne-lib:*` です。Minecraft 共通層は `:hauyne-config:minecraft` のままです。`hauyne-lib:common` は作成していません。将来追加する場合のパッケージは `dev.lapis256.hauyne.lib` とします。

公開 API は `dev.lapis256.hauyne.config`、実装は `dev.lapis256.hauyne.config.<module>` に置きます。Mod 実装は Forge・NeoForge ともに `dev.lapis256.hauyne.lib` に置きます。KSP のサービス登録先と生成ソースも、この構成に合わせています。

依存バージョンは `gradle/libs.versions.toml`、リリース情報はルートの `gradle.properties` で管理します。`modVersion` は Release workflow が更新します。Minecraft ごとの設定は各モジュールの `versions/<ターゲット>/gradle.properties` に置きます。lib と config は別々に対応対象と設定値を持ちます。Stonecutter と Toolchain resolver の設定プラグインは初期化順の都合で `settings.gradle.kts` に固定しています。

Java 17 の制約は共通 JVM ライブラリに適用します。Minecraft 側の Toolchain は共通プラグインが対応するバージョンファイルの `javaVersion` から設定します。Kotlin の共通設定で Minecraft 側を Java 17 に固定することはありません。

例えば `hauyne-config/minecraft/versions/26.1/gradle.properties` は次の内容です。

```properties
minecraftVersion=26.1.2
javaVersion=25
neoFormVersion=26.1.2-1
```

| 対象 | Java / JVM target |
| --- | --- |
| `hauyne-config:core`・`hauyne-config:ksp`・`hauyne-config:memory`・`hauyne-config:nightconfig` | 17 |
| `examples:jvm` | 21 |
| Minecraft 26.1.2 の共通層・NeoForge アダプター・Mod | 25 |
| Minecraft 1.21.1 の共通層・NeoForge アダプター・Mod | 21 |
| Minecraft 1.20.1 の共通層・Forge アダプター・Mod | 17 |

Minecraft の共通層は NeoForm Vanilla、1.20.1 のみ LegacyForge MCP Vanilla です。アダプターはローダーごとに分けています。現在の Minecraft バージョンは `stonecutter.current.version`、MC 系列を使うパスは `stonecutter.current.project`、バージョンの比較は `stonecutter.eval(...)` で扱います。プロジェクト名を Minecraft バージョンとみなす処理は使いません。

Mod 初期化クラス、仮実装、テスト、生成する定数クラスは Kotlin で記述します。Java は利用者が明示的に指定した場合に使います。Core・対応する Minecraft 共通層・アダプターを Jar-in-Jar に含めます。Kotlin ランタイムと KSP は同梱しません。開発用に必要な Kotlin クラスパスと、本番 Mod への同梱は別の設定です。本番の Kotlin ランタイム供給方法は未設定で、KotlinForForge などの必須依存も未登録です。ライブラリをロードする本番環境の動作確認は完了していません。

内部の Gradle group は同名のバージョンプロジェクト間の衝突を避けるため分けています。公開座標の groupId はすべて `dev.lapis256` です。artifactId はプロジェクトパスをハイフンでつないで生成し、Stonecutter のバージョンノードでは controller のパスを使います。例は `hauyne-config-core`、`hauyne-config-minecraft`、`hauyne-lib-neoforge` です。`hauyne-config:memory` は公開しません。

### バージョンの採番

プロジェクト全体の基本バージョン `x.y.z` は、ルートの `gradle.properties` の `modVersion` だけで定義します。Gradle のルートプロジェクトはこの値をバージョンに設定し、各モジュールと成果物の収集処理はルートプロジェクトのバージョンを参照します。MC に依存しない独立ライブラリは `x.y.z`、MC に依存するライブラリと Mod 本体は `<MC 系列>-x.y.z` を自動で組み立てます。リリースタグは `vx.y.z` とし、Release workflow が同じ `modVersion` との一致を検証します。例えば `modVersion=0.1.0` のとき、独立ライブラリは `0.1.0`、リリースタグは `v0.1.0` です。

MC 系列は基本的にメジャーとマイナーの2要素で表します。26 より前のバージョンでは先頭の `1.` を省きます。26 以降は各系列の最新パッチバージョンだけをサポートし、それ以前のパッチバージョンはサポートしません。例えば `26.1` の対象が `26.1.2` のとき、`26.1` と `26.1.1` はサポート対象外です。Gradle のターゲット名は全モジュールで `20.1`・`21.1`・`26.1` の形式に揃えます。ローダーは親の controller のパスで区別します。成果物のバージョンには従来どおり MC 系列を使います。

| Minecraft | MC 系列 | modVersion が `0.1.0` のとき |
| --- | --- | --- |
| 1.20.1 | `20.1` | `20.1-0.1.0` |
| 1.21.1 | `21.1` | `21.1-0.1.0` |
| 26.1.2 | `26.1` | `26.1-0.1.0` |

すべての MC 系列とモジュールで同じ `x.y.z` を参照し、系列やモジュールごとの独立した採番は行いません。同じ `x.y.z` でも MC 系列間のバイナリ互換性は意味しません。Minecraft の依存範囲と Toolchain の指定には完全な Minecraft バージョンを使います。

## ローカル検証

Windows では `./gradlew` を `./gradlew.bat` に読み替えてください。

対応対象と既定ターゲットは `settings.gradle.kts` の controller ごとに宣言します。アダプターには同じ系列の共通層、lib には同じローダー・系列のアダプターと共通層が必要です。設定ファイルも各モジュールに置きます。

```text
hauyne-config/minecraft/versions/21.1/gradle.properties
hauyne-config/neoforge/versions/21.1/gradle.properties
hauyne-lib/neoforge/versions/21.1/gradle.properties
```

ターゲットを追加するときは、その controller の宣言と設定ファイルを追加します。アダプターには `minecraft-config-backend`、lib には `mod` を適用します。プラグインがプロジェクト名から依存先を自動設定します。たとえば `21.1` は共通層の `21.1` を参照し、lib は同名の NeoForge アダプターを参照します。lib には Core・共通層・アダプターを Jar-in-Jar として同梱します。対応するプロジェクトがなければ設定時に失敗します。lib に別のローダーを追加しても、config のバニラモードや対応対象は変えません。登録済みの全ターゲットを `check` で検証し、公開プラグインを適用した全モジュールを本番 Release に含めます。

| タスク | 用途 |
| --- | --- |
| `check` | 全モジュールのテストと整形チェック。別ビルドの `build-logic` は `:build-logic:test` で指定 |
| `prepareJvmPublications` | 公開する JVM ライブラリを Maven local に配置 |
| `recordReleaseArtifacts` | 公開対象を生成し、Maven local との照合、Mod 配布の dry-run、成果物の収集を実行 |

リリース用の成果物は `.release/assets/` に収集します。Forge は再難読化後の配布 JAR を使います。

```sh
./gradlew help projects
./gradlew :build-logic:test check
./gradlew build test publishToMavenLocal
./gradlew :hauyne-lib:forge:20.1:check
```

CI はテストと整形チェックを実行します。公開可能かの確認は Release workflow の dry-run で行い、CI ではローカル公開や配布 dry-run を実行しません。

Gradle の設定、標準タスク、ディレクトリ構成、ファイルの存在を細かく確認するセットアップ用テストは追加しません。ビルドやコンパイルで検出できる問題は、通常の Gradle タスクで検証します。独自のビルド処理や通常のビルドでは検出できない重要な仕様に限り、具体的な不具合を検出する最小限のテストを追加します。

```sh
./gradlew :hauyne-config:minecraft:20.1:build :hauyne-config:forge:20.1:build :hauyne-lib:forge:20.1:build
./gradlew :hauyne-config:minecraft:20.1:publishToMavenLocal :hauyne-config:forge:20.1:publishToMavenLocal :hauyne-lib:forge:20.1:publishToMavenLocal
./gradlew :hauyne-lib:forge:20.1:runServer
```

Forge の配布ファイルには `reobfJar` の出力を使います。開発用 JAR は `build/devlibs`、配布用は `build/libs` です。Mixin と Access Transformer は使用していません。

Stonecutter の切替は controller ごとに行います。依存先はプロジェクト名から決まるため、他の controller の選択を揃える必要はありません。

```sh
./gradlew ":hauyne-lib:neoforge:Set active project to 21.1"
./gradlew :hauyne-lib:neoforge:21.1:build
./gradlew :hauyne-lib:neoforge:21.1:runServer
./gradlew :hauyne-lib:forge:20.1:build
```

既定アクティブプロジェクトは共通層が `26.1`、NeoForge が `26.1`、Forge が `20.1` です。各モジュール直下の `src/` は共有ソース、各モジュールの `versions/<ターゲット>/src/` はバージョン別ソースです。共有ソースと同じ相対パスにバージョン別ソースを置くと、そのバージョンの実装を上書きできます。バージョン別ソースは Git の管理対象です。Stonecutter の生成先は `build/generated/stonecutter/` で、通常の `build/` 除外に含まれます。

バージョン別設定の読み込みも既存機能を確認しています。Stonecutter 0.10 の構造化プロパティは JSON・TOML・YAML 用で、今回指定された `gradle.properties` は対象外です。そのため、ここだけは Java 標準の `Properties` と Gradle の `providers.fileContents` を使っています。独自のバージョン比較やファイル解析器は実装していません。

## 認証情報とプロジェクト ID

GitHub の `release` Environment に承認ルールを設定してください。リポジトリのファイルだけでは環境保護を有効化できません。

リポジトリの Settings → General → Releases で `Enable release immutability` を有効にしてください。Release workflow は既定ブランチへのバージョン更新コミットとタグの push を行います。ブランチ保護や Ruleset を設定している場合は、GitHub Actions にこの更新を許可してください。

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
./gradlew :hauyne-config:minecraft:26.1:publish :hauyne-config:neoforge:26.1:publish :hauyne-lib:neoforge:26.1:publish "-Dorg.gradle.s3.endpoint=$R2_ENDPOINT"
./gradlew :hauyne-lib:neoforge:26.1:publishMods -PmodPublishDryRun=false
```

1.21.1 も同じ手順で、プロジェクトパスの Minecraft バージョンを置き換えます。配布サービス向けのバージョンも `<MC 系列>-x.y.z` を使います。公開チャンネルは `releaseChannel` の `ALPHA`・`BETA`・`STABLE` で指定します。

CI は1つのジョブで `./gradlew :build-logic:test check` を実行し、ビルド処理のテスト、JVM と全 Minecraft 系列の `check`、整形チェックを行います。プロジェクト間の並列実行は、既存の `org.gradle.parallel=true` 設定で Gradle に任せます。Release workflow はテストを実行せず、バージョンとチェンジログの生成、成果物の生成・照合、配布 dry-run を行います。本番では続けて、コミットとタグの push、構成証明の登録、GitHub Draft Release への添付、Maven・CurseForge・Modrinth への公開、Draft の公開の順に処理します。

### GitHub Actions からのリリース

Actions → Release → Run workflow で実行します。本番公開は既定ブランチからのみ実行でき、dry-run は作業ブランチでも実行できます。

| 入力 | 値 | 動作 |
| --- | --- | --- |
| `bump` | `auto` | git-cliff が Conventional Commits から更新幅を判定 |
| `bump` | `major` / `minor` / `patch` | 指定した桁を更新 |
| `dry_run` | `true` | チェンジログ生成、ビルド、Maven ローカル公開、CF/Modrinth の dry-run、成果物の照合 |
| `dry_run` | `false`、または省略 | 検証後に GitHub・CurseForge・Modrinth・Maven へ公開 |

`bump` の既定値は `auto` です。`fix:` はパッチ、`feat:` はマイナー、破壊的変更はメジャーを進めます。0.x でも同じ規則を使います。タグのない初回の `auto` は `gradle.properties` の現在値を使い、明示指定ではその現在値から指定した桁を進めます。2回目以降は最新タグと `modVersion` の一致を要求します。

git-cliff 2.14.2 と `cliff.toml` から、全リリースを含む `CHANGELOG.md` と、そのバージョンだけの `.release/notes.md` を生成します。GitHub Releases・CurseForge・Modrinth は同じリリースノートを使います。Actions のサマリにはリリース予定版と両方のチェンジログを表示します。

バージョン更新とチェンジログ生成は Kotlin の `prepareRelease` タスク、成果物の照合と記録は `recordReleaseArtifacts` タスクで実装しています。どちらも `build-logic` の `release` Convention Plugin にまとめています。ローカルで準備する場合も git-cliff 2.14.2 を PATH に配置し、次の順に実行してください。`prepareRelease` は作業ツリーの `gradle.properties` と `CHANGELOG.md` を更新します。

```sh
./gradlew prepareRelease -PreleaseBump=auto
./gradlew recordReleaseArtifacts -PmodPublishDryRun=true -PreleaseChangelog=.release/notes.md
```

dry-run ではリポジトリへのコミット・タグの push、GitHub Release 作成、外部公開、構成証明の登録を行いません。`release` Environment の保護ルールは dry-run にも適用されます。生成物と dry-run の出力は Actions の Artifact に保存します。

認証情報の有無だけを調べる独自タスクは設けません。認証と公開権限は本番公開時に公開先の処理で確認します。dry-run は実際のアップロードを行わないため、認証や公開権限が有効であることまでは確認できません。

本番では登録された全公開対象を検証し、バージョンと `CHANGELOG.md` をコミットして `v<modVersion>` タグとともに push します。すべての公開ファイルに構成証明を登録し、GitHub Draft Release に添付します。Maven・CF・Modrinth への公開が完了したら Draft を公開します。Immutable Release に後からファイルを追加する処理や `--clobber` による上書きは使いません。

### 成果物の一致と利用者による検証

同じジョブの検証済み成果物を全公開先で使います。各モジュールの Maven Publication に登録された JAR・sources JAR・POM・Gradle Module Metadata をローカル Maven 公開物とバイト単位で照合し、GitHub 添付用にコピーします。Mod の配布ファイルも Maven 公開対象に含まれることを確認します。Mod の JAR は Maven・GitHub・CF・Modrinth で同じファイルです。公開前に各ファイルの SHA-256 を照合し、内容の変更や未記録の公開ファイルを検出した場合はそのタスクを停止します。

GitHub Release には全公開ファイルと `SHA256SUMS` を添付します。`actions/attest` で各ファイルのビルド構成証明を登録するため、公開先から取得したファイルを利用者が検証できます。Workflow 内での公開後の検証は行いません。

```sh
# GitHub・CF・Modrinth・Maven から取得したファイルのビルド構成証明
gh attestation verify ./hauyne-lib-neoforge-26.1-0.1.0.jar -R Lapis256/HauyneLib

# Immutable Release と、その添付ファイルの構成証明
gh release verify v0.1.0 -R Lapis256/HauyneLib
gh release verify-asset v0.1.0 ./hauyne-lib-neoforge-26.1-0.1.0.jar -R Lapis256/HauyneLib

# 全添付ファイルと SHA256SUMS を同じディレクトリに取得した場合
sha256sum --check SHA256SUMS
```

構成証明の生成には Workflow の `id-token: write` と `attestations: write` を使います。Release immutability はリポジトリ設定が前提です。[GitHub の構成証明の手順](https://docs.github.com/ja/actions/how-tos/secure-your-work/use-artifact-attestations/use-artifact-attestations) と [Immutable Release の仕様](https://docs.github.com/en/code-security/concepts/supply-chain-security/immutable-releases) を参照してください。

### 公開途中で失敗した場合

外部サービスをまたぐ公開は一括で取り消せません。失敗時は各サービスの公開状況と、保存された Actions Artifact を確認してください。GitHub は公開前なら Draft のまま残ります。タグとバージョン更新コミットが残る場合もあります。公開済みバージョンへの再登録はサービス側で拒否される可能性があるため、公開ジョブ全体を無条件に再実行しないでください。復旧には保存済みの同じ成果物を使い、再ビルドしたファイルで置き換えないでください。

## 検証の範囲

ゲームサーバーの EULA 同意は自動設定しません。Forge の起動試行は EULA 確認までであり、Mod 初期化・ワールド起動・配布 JAR をインストールした本番環境での動作は未検証です。Forge の追加対応は未完了として扱います。

本番 R2 接続、CurseForge・Modrinth の実 ID と認証、GitHub Actions のホスト上での実行は未検証です。公開 API は最小サンプルであり、ライブラリ全機能の実装を意味しません。

参照した公式資料: [MDG](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)、[LegacyForge](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md)、[Mod Publish Plugin](https://modmuss50.github.io/mod-publish-plugin/options/)、[Gradle S3](https://docs.gradle.org/current/userguide/supported_repository_protocols.html)。
