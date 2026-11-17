# Domain documentation

## Location and reading order

Use a single documentation structure: the glossary lives in the root `GLOSSARY.md`, and architectural decisions live in `docs/adr/`.
Before examining code, read `GLOSSARY.md` and the ADRs relevant to the task.
If these documents do not exist, continue without reporting their absence or proposing their creation. Create them with the `domain-modeling` skill when terminology or architectural decisions are settled.

## Terminology and architectural decisions

Use glossary terms when expressing domain concepts in issue titles, improvement proposals, hypotheses, and test names.
If a needed concept is missing from the glossary, check whether the term is used in the project. If the glossary needs an addition, record it as an item for `domain-modeling`.
If a proposal conflicts with an existing ADR, identify the ADR and explain why it should be reconsidered.
