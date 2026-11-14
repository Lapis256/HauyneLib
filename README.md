# HauyneLib

Gradle マルチプロジェクトの設定ライブラリと、NeoForge / Forge 用 Mod です。

Java 25 で `./gradlew verifyRequired -PmodPublishDryRun=true` を実行すると、Minecraft 26.1.2 と 1.21.1 のビルド・テスト・ローカル Maven 公開・Mod 配布 dry-run を検証できます。Windows では `./gradlew.bat` を使ってください。

構成、Forge 1.20.1 の追加検証、認証情報、公開先ごとの再実行手順は [ビルドと公開](docs/publishing.md) を参照してください。公開済みの成果物を確認し、失敗した公開先のタスクだけを再実行してください。

Forge はゲーム起動完了まで未検証のため本番リリースには含めません。通常の Mod 公開タスクは dry-run が既定です。
