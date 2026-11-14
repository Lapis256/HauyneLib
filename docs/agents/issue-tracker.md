# 課題管理

課題と仕様は GitHub Issues（Lapis256/HauyneLib）で管理し、操作には `gh` CLI を使います。
`gh` コマンドは毎回サンドボックス外での実行を申請してください。ネットワークや認証の失敗後にサンドボックス内で再試行しないでください。

## 操作

- 作成: `gh issue create --title "..." --body-file <本文ファイル>`
- 閲覧: `gh issue view <番号> --comments`。ラベルも確認してください。
- 一覧: `gh issue list --state open --json number,title,body,labels,comments`。必要に応じて `--label` と `--state` で絞り込んでください。
- コメント: `gh issue comment <番号> --body-file <本文ファイル>`
- ラベルの追加と削除: `gh issue edit <番号> --add-label "ready for agent"` / `--remove-label "needs triage"`
- クローズ: `gh issue close <番号> --comment "..."`

複数行の本文はファイルに保存し、`--body-file` で渡してください。ラベル名は `docs/agents/triage-labels.md` に従います。
リポジトリは `git remote -v` で確認してください。クローン内では `gh` がリモートから対象を判別します。

## PR の扱い

PRs as a request surface: no.

## スキルの指示との対応

「課題管理先に公開する」は GitHub Issue の作成を意味します。
「関連するチケットを取得する」は `gh issue view <番号> --comments` を実行することを意味します。
