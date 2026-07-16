# Progress: doc-fixes

> Doc: `specs/proposals/doc-fixes/proposal.md`

## Session 2026-07-16 — implementation

### Done

All tasks T1–T6 implemented on branch `docs/accuracy-fixes`, base `develop` @
`aa31dfe`, one commit per task:

- **T1** — `app/build.gradle.kts`: signing-config guard changed from
  `if (keystorePath != null)` to `if (!keystorePath.isNullOrEmpty())`.
- **T2** — `README.md`: Features section, Koala 2024.1.1, Docker-or-Node
  prerequisite, emulator pointer, `socket_url`/server-README pointer, four
  Tech Stack bullets, corrected module graph, corrected DI wording,
  corrected UDF diagram, directory-notation Project Structure, extended
  `demo/` line, CI/CD download bullets, precise signing sentence, Testing
  dedupe, TOOLS.md link wording.
- **T3** — `README.zh-TW.md`: same 15 edits as T2, translated, 1:1 section
  structure (7 `##`, 8 `###` headings in both files). Diagrams rebuilt with
  a small CJK-aware padding script (`unicodedata.east_asian_width`) so
  box borders line up despite double-width Chinese characters — same
  border widths as the English diagrams (55/42/50), same topology.
- **T4** — `TOOLS.md`: retitled to "AI-Assisted Development", kept
  Workflow + How It Helped sections, added the Findings & Fixes table
  with all 9 rows, added the CI/CD + signing closing line.
- **T5** — `server/README.md`: opening description, Configuration table
  (`OPEN_WEATHER_API_KEY`/`PORT`/`POLL_INTERVAL_MS`), Admin & Testing
  section, expanded Events payload fields.
- **T6** — `specs/` annotations: backfill session entry + supersession
  bullets in `progress.md`; Implementation-drift blockquotes in
  `phase1/design.md` and `phase2/design.md`; Superseded blockquotes
  (×2) in `phase3/design.md`; §10/§8/§9 checklists ticked; `AI_TOOLS.md`
  → `TOOLS.md` rename notes; `2.1.2` version note in `phase3/tasks.md`;
  Status line added to `logger-module/proposal.md`; new `specs/README.md`
  index.

### Verification (real runs, not simulated)

- `KEYSTORE_PATH="" ./gradlew :app:assembleRelease` — BUILD SUCCESSFUL.
  APK verified debug-signed: `apksigner verify --print-certs` →
  `Signer #1 certificate DN: C=US, O=Android, CN=Android Debug`.
- Throwaway keystore (`keytool -genkeypair ... -dname CN=Test`) +
  `KEYSTORE_PATH=/tmp/test.keystore ... ./gradlew :app:assembleRelease`
  — BUILD SUCCESSFUL. `apksigner verify --print-certs` → `Signer #1
  certificate DN: CN=Test`.
- `./gradlew test` — BUILD SUCCESSFUL, all modules' unit tests ran/passed.
- All per-task grep acceptance checks in T1–T6 run and passed (see report
  to orchestrator for the exact commands/output).

### Deviations from the doc

1. **T1 acceptance command literal mismatch:** the doc's acceptance
   command `keytool -printcert -jarfile app-release.apk` reports "not a
   signed jar file" on both the fallback and signed builds. This is a
   tool-choice artifact, not a build defect: with `minSdk = 26`, AGP
   signs release APKs with APK Signature Scheme v2/v3 only (no v1/jar
   `META-INF/*.RSA` block), which `keytool -printcert -jarfile` cannot
   read. `apksigner verify --print-certs` (the correct tool for v2/v3)
   confirms the intended certificates (`CN=Android Debug` / `CN=Test`)
   on both builds — i.e. the underlying fix works exactly as specced,
   only the literal doc command doesn't apply to this AGP/minSdk
   combination.
2. **T6 acceptance grep self-match:** `grep -rn "AI_TOOLS.md" specs/ |
   grep -v "originally planned"` returns non-zero rows, all four inside
   `specs/proposals/doc-fixes/proposal.md` itself (the task doc's own
   prose describing the `AI_TOOLS.md` finding, e.g. line 122, 534, 552,
   554). The doc is committed verbatim per the SETUP instructions and
   is not itself stale spec content, so it was left unedited. All
   target files (`phase1/design.md`, `phase3/design.md`) are clean —
   confirmed by re-running the same grep restricted to those two files.

### Decisions made (implementation-level, non-architectural)

- zh-TW diagram alignment: used a small local Python helper
  (`unicodedata.east_asian_width`) to compute CJK-aware padding so box
  borders match the English diagrams' widths exactly; not committed
  anywhere in the repo (scratch tooling only).
- Worktree HEAD was stale at session start (`52c808c`, a diverged
  branch, not an ancestor of `develop`); checked out `aa31dfe` before
  branching, per the SETUP instructions' fallback path.

### Next

None — all six tasks complete, PR Acceptance Checklist items runnable
in this environment have been run and passed. Remaining checklist item
(independent `/review-diff`) is the orchestrator's responsibility.
