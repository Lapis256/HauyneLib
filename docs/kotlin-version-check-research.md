# Kotlin 標準ライブラリの起動時検査

HauyneLib の配布 JAR に、Kotlin を必要としない検査コードを同梱する案を調べた。利用側 Mod は `[features.<modid>]` に Maven 形式の要求範囲を書き、通常の Mod エントリポイントは Kotlin のままにする。検査対象は実行環境の `kotlin.KotlinVersion.CURRENT` とし、KotlinLangForge（KLF）は導入先として案内する。

## 対象バージョン

| プロジェクトの対象 | 確認したローダー | 種別指定のない Jar-in-Jar |
| --- | --- | --- |
| NeoForge 26.1.2.126 | FancyModLoader 11.0.15 | `LIBRARY`。FML がプラグイン用クラスローダーに追加する |
| NeoForge 21.1.262 | FancyModLoader 4.0.45 | `LIBRARY`。ModLauncher の `PLUGIN` レイヤーに載る |
| Forge 1.20.1-47.3.0 | Forge FML 47.3.0 | `GAMELIBRARY`。検査 JAR をプラグインとして載せるには、子 JAR に `FMLModType: LIBRARY` を明示する |

NeoForge の `NestedLibraryModReader` は親の有無を参照する。子 JAR のメタデータで種別が決まらなければ `LIBRARY` にする。Forge 47.3.3 からは親が `LIBRARY` または `LANGPROVIDER` の場合に限り、子の既定値も `LIBRARY` になる。親の HauyneLib は `MOD` なので、この変更だけでは検査 JAR の既定値は変わらない。 [FML 11.0.15 ソース配布物](https://maven.neoforged.net/releases/net/neoforged/fancymodloader/loader/11.0.15/loader-11.0.15-sources.jar) · [FML 4.0.45 ソース配布物](https://maven.neoforged.net/releases/net/neoforged/fancymodloader/loader/4.0.45/loader-4.0.45-sources.jar) · [Forge 47.3.0 ソース配布物](https://maven.minecraftforge.net/net/minecraftforge/fmlloader/1.20.1-47.3.0/fmlloader-1.20.1-47.3.0-sources.jar) · [Forge PR #10023](https://github.com/MinecraftForge/MinecraftForge/pull/10023)

## 登録の入口

- **NeoForge 26.1** は、内包した `LIBRARY` から `ClassProcessorProvider` を登録する案を優先する。FML はプラグインを読み込んでから `createProcessors` を呼ぶ。この呼び出しで FML の `ForgeFeature` に検出バージョンを登録できる。ゲーム用クラスローダーの構築より前だが、KLF 2.16.0 の配布物に内包された `kotlin-stdlib-2.4.21.jar` は `LIBRARY` に分類される。クラス変換のための API を起動時の登録に使うため、将来の FML 更新時は呼び出し順を再確認する。根拠は FML 11.0.15 の `FMLLoader`、`ClassProcessorSet`、`NestedLibraryModReader` と KLF の配布 JAR。
- **NeoForge 1.21.1** は、内包した `LIBRARY` から `IModLanguageLoader` を公開する案がある。FML の `LanguageProviderLoader` は `ServiceLoader` で候補を列挙してインスタンス化し、名前とバージョンを読み取る。利用側の `modLoader = "javafml"` を維持したまま登録できる見込みがある。検査専用で使わない言語ローダーを追加することになるため、実機でサービスの発見と登録順を確認する。根拠は FML 4.0.45 の `LanguageProviderLoader`、`LaunchContext`、`ServiceLoaderUtil` と `ModValidator`。
- **Forge 1.20.1** は、明示的に `LIBRARY` とした子 JAR から `IModLanguageProvider` を公開する案がある。`LanguageLoadingProvider` は `PLUGIN` レイヤーにあるプロバイダーを列挙し、名前と JAR の `Implementation-Version` を取得する。KLF 2.0 系のビルドは標準ライブラリを `LANGPROVIDER` の本体に含める。この場合も登録時にはプラグイン側の Kotlin が検査対象になる。Java 17 で内包 JAR のサービスが見つかることと、KLF のクラスが見えることを確認する。根拠は Forge 47.3.0 の `LanguageLoadingProvider`、`ModValidator` と `AbstractModProvider`。

公開されている KLF のビルド定義では、Forge 向け 2.0 系は Shadow を使い、NeoForge 1.21.1 向け 3.0 系と 26.1 向け 3.1 系は依存を内包する。1.21.1 では、内包された標準ライブラリのマニフェストと JPMS の可視性を配布物で確認する。 [KLF のビルド定義](https://github.com/btwonion/KotlinLangForge/blob/45ce0b7/klf/build.gradle.kts#L57-L85) · [KLF の対応バージョン](https://github.com/btwonion/KotlinLangForge#versioning)

## 代替経路と制約

- 言語ローダーを正式に提供して `javafml` 相当の処理へ委譲する案では、利用側も `modLoader` の変更が必要になる。検査だけを導入する場合の第一候補は、従来の `javafml` を維持できる上記の経路。
- 26.1 の `IModFileCandidateLocator` など、Mod 検出前に見つけるサービスを使う場合は、Jar-in-Jar の解決を待てない。別のトップレベル JAR が必要になる。標準ライブラリがゲーム側にだけ載る環境を支援する場合は、ゲーム用クラスローダーの構築後、`ModLoader.gatherAndInitializeMods` の検査前に登録する経路が必要になる。Mixin やクラス変換による挿入は候補にできるが、ローダー内部のメソッドと実行順への依存が増える。
- 通常の `@Mod` コンストラクターやライフサイクルイベントは登録期限より後に実行される。`modproperties` に戻す場合も、初期化前に読み取って検査する入口は必要になる。
- FML の標準エラーには要求元、要求範囲、検出値が入る。KLF の導入案内を出す場合は、既存の表示文に案内を追加できるか、追加の読み込みエラーを登録する必要があるかを確かめる。NeoForge には `ModLoader.addLoadingIssue` があるため、登録済みの Mod メタデータを参照できる段階なら候補になる。Forge では標準の `ModLoadingException` 経路と表示の確認が必要。
- Java の検査コードなら検査専用の標準ライブラリは不要。Kotlin とリロケーションを使う案では、標準ライブラリ 2.4.21 の単体 JAR は約 1.77 MiB。縮小後のサイズは未計測。標準ライブラリを Jar-in-Jar で一般に提供する案は、KLF 併用時の依存選択も検証する必要がある。

## 実行結果と未検証項目

FML 11.0.15 と Kotlin 標準ライブラリ 2.4.21 の実 JAR を使い、隔離したクラスローダーから `KotlinVersion.CURRENT` を取得できることを確認した。クライアントと専用サーバーの両側で `[2.4.21,)` を通過し、`[2.4.22,)` は拒否された。機能を登録しない状態では拒否され、検出値は `NONE` だった。

配布 JAR を使ったゲーム起動は未検証。各系列で `mods` に HauyneLib の配布 JAR を１本だけ入れて検査サービスの発見を確かめる。次に、Kotlin の未導入、範囲外、範囲内の３条件で、クライアントの標準エラー画面と専用サーバーのログを確認する。範囲内では利用側 Mod の構築まで進み、停止時には要求元、要求範囲、検出値、導入案内が見えることを確認する。
