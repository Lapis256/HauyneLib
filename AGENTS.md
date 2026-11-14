<!-- trail:start -->
## Trail — repo context graph

The Trail context graph is in `graft/`: linked Markdown nodes that explain each
system and carry exact file:line spans. `.trail/cache/` holds the local index used by the Trail CLI.

For ANY task here — understanding how something works, finding where code lives,
or scoping a change — get context from the graph before grepping or opening
source files. Re-ask freely (it's cheap) and reuse literal identifiers you
already have (symbol, error string, file name) as the query. New to this repo?
Run `trail map` first — a token-budgeted orientation (dir clusters, hubs,
hotspots), no LLM, no key.

- Run `trail ask "<your question>" --source` → ranked nodes with the relevant
  code spans inlined (each hit's ≤8-line crux by default; `--full` for whole
  definitions when the crux isn't enough). Match the tool to the task shape:
  for understanding or editing, the top node IS the answer — cite its
  `covers:` file:line spans and edit straight from `--source`. For
  exhaustive tasks ("every occurrence / every caller of this pattern"), ranked
  results are top-N, not complete — run `trail grep "<literal>"` instead
  (exhaustive over indexed files, grouped by enclosing symbol), falling back
  to raw `grep -rn` only for unindexed files.
- `trail skeleton <file>` → every definition's signature + span, ~10× cheaper
  than reading the file; use it to skim an API surface.
- `trail callers <symbol>` gives precomputed, exact edges — who calls this.
  Add `--direction out` for what it calls, or `--depth N` to walk
  transitively for the full blast radius. For structural questions, skip
  ranking and use this directly.
- Or browse: `graft/INDEX.md` lists every node; follow the links.
- Monorepos and folders of multiple repos rank fairly across sub-projects —
  hits carry `[scope/]` labels naming which one they're from. Narrow with
  `trail ask "<task>" --in <scope>/` once you know where you're working.

If a returned span is truncated ("+N more lines"), open the file at that exact
range before finalizing. Only open source files when a node genuinely lacks a
needed detail, and then at the exact file:line the node points to — never
re-read whole files.

After big code changes, refresh the graph with `trail build` (deterministic,
no API key, $0).
<!-- trail:end -->

## Writing conventions

- Use English as the primary language for public-facing essentials, such as the README and public API documentation.
- Write KDoc in both English and Japanese, with English first.
- Write code comments and internal development documentation in Japanese, prioritizing the Japanese-speaking maintainer's readability.
- Write commit messages and pull request titles in English using Conventional Commits.
- Keep both concise while conveying the necessary scope; commit messages should normally consist of the subject line only.
- Write all agent-facing documentation in English as an exception to the internal documentation rule.

## Commit workflow

On any branch other than the default branch, treat explicit or implicit requests to change files as including a request to commit the completed changes. After appropriate verification, create focused commits containing only the task's changes without waiting for a separate commit instruction.

## Agent skills

### Issue tracker

Track issues and specifications in GitHub Issues (`Lapis256/HauyneLib`). Read `docs/agents/issue-tracker.md` before working on issues.

### Triage labels

GitHub label names use spaces. Read the mapping in `docs/agents/triage-labels.md` before classifying issues.

### Domain docs

Domain documentation uses a single structure. Follow `docs/agents/domain.md` to read the glossary and relevant ADRs before examining code.
