# メタデータ DSL の設計案

依存 MOD の実行時メタデータと配布先の関係設定を、一つの宣言から生成する。以下は実装済みの DSL の仕様である。

## 設定例

```kotlin
import dev.lapis256.hauyne.buildlogic.metadata.HauyneDependencyOrdering.AFTER
import dev.lapis256.hauyne.buildlogic.metadata.HauyneDependencySide.BOTH
import dev.lapis256.hauyne.buildlogic.metadata.HauyneFmlLoader

hauyneMetadata {
    modId = "example"
    modName = "Example"
    modPackage = "dev.example"
    license = "MIT"
    description = "An example mod"

    forgeLike {
        loader = HauyneFmlLoader.NEOFORGE
        minecraftVersionRange = "[1.21.1,1.22)"
        loaderVersionRange = "[21.1,)"
        javaFmlLoaderVersionRange = "[4,)"

        dependencies {
            require("example_api") {
                versionRange = "[1.0,2.0)"
                reason = "Required for configuration integration"
                ordering = AFTER
                side = BOTH
                curseForgeSlug = "example-api"
                modrinthSlug = "example-api"
            }
            optional("example_integration")
        }
    }
}
```

ID とバージョンは説明用の値である。Gradle の `Property` と `Provider` を使い、固定値だけでなく既存のプロパティからも設定できる形にする。

## 共通設定と移行

- `hauyneModMetadata` の項目を `hauyneMetadata` 直下へ移す。`gradle.properties` からの既定値は維持する。
- `hauyneFmlMetadata` の項目を `forgeLike` へ移す。Forge と NeoForge の選択は `loader` で行う。
- Minecraft・ローダー・JavaFML のバージョン範囲は別々に指定する。Mod の公開バージョンは引き続き `project.version` を使う。
- リポジトリ内の利用箇所をまとめて移行する。内部ビルド用 DSL のため、旧 Extension の互換エイリアスは追加しない。
- 配布する自プロジェクトの ID、トークン、成果物、変更履歴、公開種別、対応環境は既存の `publishMods` に残す。依存先の関係設定だけを新 DSL から渡す。

## 依存関係

| 宣言 | 意味 | Forge | NeoForge | 配布先の既定の関係 |
| --- | --- | --- | --- | --- |
| `require(modId)` | 必須 | `mandatory=true` | `required` | 必須 |
| `optional(modId)` | 導入は任意。導入済みなら範囲を検査 | `mandatory=false` | `optional` | 任意 |
| `incompatible(modId)` | 指定範囲との併用禁止 | 構成エラー | `incompatible` | 非互換 |
| `discouraged(modId)` | 指定範囲との併用を警告 | 構成エラー | `discouraged` | 自動出力しない |

`require` と `optional` は設定ブロックを省略できる。各項目は次のように扱う。

- `versionRange` は Maven 形式の範囲文字列。省略時は全バージョンを対象とする。範囲の独自 DSL は作らない。
- `ordering` は `NONE`・`BEFORE`・`AFTER` の列挙値で、既定値は `NONE`。
- `side` は `BOTH`・`CLIENT`・`SERVER` の列挙値で、既定値は `BOTH`。依存を検査する側を表し、配布する Mod 自身の対応環境は変更しない。
- `reason` は任意の説明。NeoForge の依存メタデータへ出力する。Forge では TOML コメントとして保存し、ローダーによる表示は保証しない。
- Minecraft とローダーの必須依存は引き続き自動生成する。その MOD ID の手動宣言、自己依存、同一 MOD ID の重複宣言は構成エラーにする。
- Gradle のコンパイル依存や Jar-in-Jar は自動追加しない。

## 配布先の指定

Modrinth API の依存関係には `project_id` と任意の `version_id` があり、関係種別は required・optional・incompatible・embedded である。[公式 API](https://docs.modrinth.com/api/operations/createversion/)

CurseForge Upload API の関係設定には slug と、完全一致用の任意の `projectID` がある。ただし、この仕様だけでは ID 単独での指定可否は断定しない。[公式 Upload API](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api)

既存の Mod Publish Plugin は Modrinth の依存先に ID または slug を指定できる。CurseForge の依存先は、確認した upstream 実装で slug が必須である。[Modrinth の設定](https://modmuss50.github.io/mod-publish-plugin/platforms/modrinth/)・[CurseForge の実装](https://github.com/modmuss50/mod-publish-plugin/blob/main/src/main/kotlin/me/modmuss50/mpp/platforms/curseforge/Curseforge.kt)

依存先は両サービスとも slug をプロパティで指定する。

- `curseForgeSlug`・`modrinthSlug` でサービスごとに指定する。同じ slug でもそれぞれに設定する。両サービスにまとめて設定する共通プロパティは設けない。
- 依存先の `projectId` と配布先の設定ブロックは設けない。Modrinth の配布版を固定する場合は `modrinthVersionId` を使う。
- `require("...")` などの引数は常にゲーム内の MOD ID。配布先 ID から推定しない。
- slug を指定していないサービスには関係設定を出力しない。明示的な空文字列はエラーにする。
- `require`・`optional`・`incompatible` の関係は自動変換する。必要な場合だけ `curseForgeRelation`・`modrinthRelation` で `REQUIRED`・`OPTIONAL`・`INCOMPATIBLE`・`EMBEDDED` を指定できる。対応する slug のない関係指定はエラーにする。
- `discouraged` に配布先を指定する場合は、そのサービスの関係種別を必須にする。警告を併用禁止へ勝手に変換しない。
- `versionRange` と `side` と `ordering` は配布先の関係設定へ変換しない。`modrinthVersionId` はゲーム内のバージョン範囲とは別に扱い、Modrinth の slug とともに指定する。
- `EMBEDDED` は配布先での表示だけを設定する。実際の同梱は既存の Jar-in-Jar 設定で行う。
- 通常の構成・メタデータ生成ではネットワーク照会を行わない。ID や slug の実在確認は配布時のサービス側検証に任せる。

自プロジェクトの公開先 ID は両サービスとも既存の `publishMods` で指定できる。これは依存先の指定とは別である。

## 実装範囲と検証

変更対象は Extension、`GenerateFmlModMetadata`、シリアライズ用モデル、`mod.gradle.kts`、各 Mod の設定、既存のメタデータ生成テスト、ビルド・公開ドキュメントとする。依存宣言は生成タスクの入力として追跡する。

検証では Forge・NeoForge の生成 TOML、任意依存の省略形、重複や未対応の宣言によるエラー、配布 dry-run の関係設定を確認する。生成 TOML の比較で既存の必須依存も維持されていることを確認する。

Forge と NeoForge の依存形式には差がある。未対応の宣言を別の意味で出力しない。[Forge の仕様](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/modfiles/)・[NeoForge の仕様](https://docs.neoforged.net/docs/gettingstarted/modfiles/)

調査日: 2026-11-16。配布プラグインの情報は upstream の公式ドキュメントと main ブランチで確認し、採用版 2.2.1 のクラス定義とも照合した。実際のアップロードは未検証である。
