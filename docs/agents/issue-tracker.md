# Issue tracking

Track issues and specifications in GitHub Issues (`Lapis256/HauyneLib`) using the `gh` CLI.
Always request execution outside the sandbox for `gh` commands. Do not retry inside the sandbox after a network or authentication failure.

## Operations

- Create: `gh issue create --title "..." --body-file <body-file>`
- View: `gh issue view <number> --comments`. Also check the labels.
- List: `gh issue list --state open --json number,title,body,labels,comments`. Filter with `--label` and `--state` as needed.
- Comment: `gh issue comment <number> --body-file <body-file>`
- Add or remove labels: `gh issue edit <number> --add-label "ready for agent"` / `--remove-label "needs triage"`
- Close: `gh issue close <number> --comment "..."`

Save multiline bodies to a file and pass it with `--body-file`. Follow `docs/agents/triage-labels.md` for label names.
Check the repository with `git remote -v`. Inside a clone, `gh` determines the target from the remote.

## Pull requests

PRs as a request surface: no.

## Mapping skill instructions

"Publish to the issue tracker" means creating a GitHub Issue.
"Fetch the relevant ticket" means running `gh issue view <number> --comments`.
