# specs/

This directory is the project's planning and history record, not live
documentation of current behavior.

- **`phase1/`, `phase2/`, `phase3/`** — point-in-time design + task docs
  for the three original build phases (MVP, Log Kit + Feature Toggle,
  Socket.IO Realtime). They are never rewritten to match later code;
  where the implementation has since drifted from what a doc describes,
  an `> **Implementation drift**` or `> **Superseded**` blockquote is
  appended next to the relevant decision instead.
- **`progress.md`** — the session log: what was done, in what order,
  and why, across the whole project. Append-only; later sessions record
  corrections and backfills rather than editing earlier entries.
- **`proposals/`** — refactor/enhancement proposals for work that came
  after the three phases (e.g. the logger module rework, this
  doc-fixes round). Each carries a `Status:` line.

Suggested reading order for someone new to the project: `progress.md`
first for the overall arc, then the phase `design.md` files in order
for the original architecture (reading the drift notes as you go), then
`proposals/` for what changed afterward.

**Language:** all docs are in English, except the Phase 1 `design.md`
Goal line and the logger module proposal
(`proposals/logger-module/proposal.md`), which are in Chinese.
