# トリアージラベル

スキルで使う役割名と GitHub 上のラベル名を次のように対応させます。

| スキル内の役割名 | GitHub のラベル名 | 意味 |
| --- | --- | --- |
| `needs-triage` | `needs triage` | メンテナーによる評価が必要 |
| `needs-info` | `needs info` | 報告者からの追加情報を待っている |
| `ready-for-agent` | `ready for agent` | 仕様が固まり、エージェントが自律的に実装できる |
| `ready-for-human` | `ready for human` | 人による実装が必要 |
| `wontfix` | `wontfix` | 対応しない |

スキルが役割名でラベルを指定した場合は、対応する GitHub のラベル名を使ってください。空白を含むラベル名はコマンドの引数で引用符で囲んでください。
