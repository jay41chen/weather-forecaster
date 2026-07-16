# AI-Assisted Development

This project was built with [Claude Code](https://docs.anthropic.com/en/docs/claude-code) — Anthropic's agentic coding tool — used across the whole development workflow: code review, architectural discussions, implementation, and documentation. Rather than a list of tools, this document is structured by that workflow, since one tool was used consistently throughout.

## Workflow

All changes followed a **Think → Do** workflow:
1. **Think**: Discuss the finding, explore alternatives, and agree on an approach before writing any code. Findings are prioritized before implementation starts.
2. **Do**: Implement the agreed-upon change as a minimal, focused commit. Each fix is committed independently with a descriptive message, so the history reads as one change per finding.

## How It Helped

- **Code Review**: Performed a structured review of the codebase across multiple dimensions (correctness, concurrency, architecture, test coverage). Each finding was discussed, prioritized, and resolved independently.
- **Architectural Refactoring**: Guided the extraction of business rules (cache TTL, per-city dedup) from `WeatherRepositoryImpl` into a dedicated `SyncWeatherUseCase`, following Clean Architecture principles — keeping the repository as a pure data-access layer.
- **Concurrency Fixes**: Identified race conditions (e.g., non-atomic `ConcurrentHashMap.getOrPut` vs `computeIfAbsent`, startup race in feature toggle initialization) and implemented targeted fixes.
- **Test Coverage**: Generated unit tests across all layers — ViewModels, use cases, and repositories — growing the test suite from 5 to 12 test files.
- **Ports & Adapters Compliance**: Relocated adapter implementations (e.g., `RemoteFeatureToggleAdapter`) from the core module to `core:data` to maintain proper dependency direction.

## Findings & Fixes

| Finding | Severity | Fix commit |
|---|---|---|
| Concurrency & lifecycle issues (non-atomic `getOrPut`, feature-toggle startup race) | major | `b001399` |
| Socket.IO reliability & data integrity | major | `cd559f2` |
| Business rules (TTL, per-city dedup) living in the repository layer | major | `3a6764c` (refactor to Clean Architecture, extracted `SyncWeatherUseCase`) |
| Test coverage gaps — suite grown 5 → 12 test files | major | `4e4285a` |
| Feature-flag properties not reactive | minor | `4270cbd` |
| Missing LazyColumn item keys (recomposition stability) | minor | `371c8cd` (PR #1) |
| Dead `isOffline` field / `showOfflineBanner` | minor | `be46246` (PR #2) |
| `ApplicationScope` on `Main.immediate` instead of `Default` | minor | `71d4193` (PR #3) |
| `BACKOFF_DELAYS` index out of bounds | minor | `268cb3d` (PR #4) |

CI/CD (`3ebd2b4`) and release signing/R8/AAB (`7ff0115`, `ab96dda`) were also built in this workflow.
