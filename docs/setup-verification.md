# セットアップ検証記録

2026年11月13日に Windows 上で検証しました。本番アップロードは実行していません。

以下は過去のセットアップ検証の記録です。2026年11月15日に、Python による成果物検査とセットアップ確認だけを目的としたテストを削除しました。

## 実行コマンドと結果

| コマンド | 結果 |
| --- | --- |
| `./gradlew.bat help projects` | 成功。通常モジュールと Minecraft 3バージョンのパスを確認 |
| `./gradlew.bat build test publishToMavenLocal` | 成功。JVM/KSP、NeoForge 2バージョン、Forge 1.20.1 を含む |
| `./gradlew.bat :hauyne-lib:neoforge:26.1.2:publishMods :hauyne-lib:neoforge:1.21.1:publishMods :hauyne-lib:forge:1.20.1:publishMods -PmodPublishDryRun=true` | 成功。全3成果物で CurseForge と Modrinth の dry-run を確認 |
| 当時の成果物検査スクリプト（削除済み） | 成功。JAR、Java 対象、Jar-in-Jar、POM、metadata を確認 |
| 検証用の別 Gradle プロジェクトで `resolve2612 resolve1211 resolve1201` | 成功。Maven local から対応する Minecraft 共通層と Core を推移的に解決 |
| Stonecutter の `Set active project to 1.21.1`、1.21.1 の `build`、`Reset active project` | 成功。Minecraft 共通層・NeoForge アダプター・Mod の controller を切替後、26.1.2 に復帰 |
| `./gradlew.bat :hauyne-lib:neoforge:26.1.2:validateModRelease -PmodPublishDryRun=false` | 想定どおり失敗。未設定の `curseforgeProjectId` を検出。アップロードなし |
| `./gradlew.bat :hauyne-config:core:publishMavenPublicationToR2Repository` | 認証情報を検証プロセス内で除去し、通信前に想定どおり失敗。アップロードなし |
| `./gradlew.bat :hauyne-lib:forge:1.20.1:runServer` | 命名整理前の起動試行は EULA 確認まで到達。整理後は未実行 |
| `javap -c` で Forge 用 Minecraft 共通 JAR を検査 | `BlockPos` への呼び出しが SRG 名 `m_123344_` に変換されていることを確認 |
| `actionlint 1.7.12 -shellcheck= -pyflakes= .github/workflows/ci.yml .github/workflows/release.yml` | 成功。ShellCheck と pyflakes は実行していない |

JUnit では Core の既定値、TOML の保存と再読み込み、KSP 生成コードの利用、Minecraft 座標の文字列化、ローダー設定の構築と未定義キーの拒否、Mod メタデータの同梱を検証しました。

build-logic の責務を分けた後も、全3バージョンのビルド・テスト・ローカル Maven 公開・配布 dry-run を再実行しました。class ファイルの検査では、共通 JVM ライブラリが Java 17、JVM 利用例が Java 21、Minecraft 側がバージョンに応じて Java 25・21・17 になっていることを確認しました。

2026年11月14日の命名整理後は `hauyne-config/`・`hauyne-lib/` と新しい Gradle パスで `clean build test publishToMavenLocal` および全3バージョンの Mod 配布 dry-run を実行しました。実装パッケージの移動、KSP のサービス登録先、ローダー別 Mod パッケージ、公開 groupId・artifactId・バージョン別の依存座標を成果物から検査しています。artifactId は共通プラグインでプロジェクトパスから生成し、ルートの横断設定は撤去しました。

Minecraft ごとの設定を `versions/<Minecraft>/gradle.properties` に移した後も、全3バージョンのビルド・テスト・ローカル公開・配布 dry-run と成果物検査が成功しました。Java の対象は各バージョンファイルの `javaVersion` に記載しています。

Kotlin の Jar-in-Jar を撤去し、成果物検査にも Kotlin ランタイムが同梱されていないことの確認を追加しました。版の取得と比較は Stonecutter 0.10 の `current.version` と `eval` に置き換えています。公式ソースで版別 override と生成先を確認し、一時的な 1.21.1 専用ソースが 1.21.1 だけにコンパイルされること、26.1.2 に混ざらないこと、Git の除外対象でないことを検証しました。検証用ソースは削除済みです。

共通ビルドスクリプトを `build-logic` に集約し、型付き Extension に移行した後も、全3バージョンの整形チェック・ビルド・テスト・ローカル公開・配布 dry-run と成果物検査が成功しました。Configuration Cache の保存と再利用も確認しました。Mod のローダー指定を一時的に省いた検査では、推測で補われず構成エラーになることを確認し、その後に元の設定を復元しました。公開前の認証検査も型付きタスクに移し、プロジェクト ID 未設定時に拒否することを確認しています。

Gradle 9.2.1 は Stonecutter 0.10 の最低要件を満たさず、初期化に失敗しました。採用版は Gradle 9.7.1、Kotlin 2.2.21、KSP 2.2.21-2.0.4、MDG 2.0.148、JUnit 6.0.1、Mod Publish Plugin 2.2.1 です。検証途中に見つかったモジュール間のパッケージ衝突、Forge 公開依存の欠落、TOML 保存の競合は修正しました。

## 残っている確認

Forge は EULA 同意後の Mod 初期化・ワールド起動と、配布 JAR を本番用ローダーへインストールした動作が未検証です。NeoForge のゲーム起動、R2・CurseForge・Modrinth への本番アップロード、GitHub Actions のホスト上での実行も未検証です。

Kotlin を同梱しない構成での本番ランタイム供給方法は未設定です。ビルド・テスト・配布 dry-run の成功は、Kotlin ランタイムなしで本番動作することを意味しません。

公開前に実在する CurseForge / Modrinth のプロジェクト ID、各サービスの認証情報、R2 エンドポイントとバケット、GitHub の `release` Environment 保護を設定してください。
