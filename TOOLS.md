# Tools Used

## Claude Code (AI-Assisted Development)

[Claude Code](https://docs.anthropic.com/en/docs/claude-code) — Anthropic's agentic coding tool — was used throughout this project for code review, architectural discussions, and implementation support.

### How It Helped

- **Code Review**: Performed a structured review of the codebase across multiple dimensions (correctness, concurrency, architecture, test coverage). Each finding was discussed, prioritized, and resolved independently.
- **Architectural Refactoring**: Guided the extraction of business rules (cache TTL, per-city dedup) from `WeatherRepositoryImpl` into a dedicated `SyncWeatherUseCase`, following Clean Architecture principles — keeping the repository as a pure data-access layer.
- **Concurrency Fixes**: Identified race conditions (e.g., non-atomic `ConcurrentHashMap.getOrPut` vs `computeIfAbsent`, startup race in feature toggle initialization) and implemented targeted fixes.
- **Test Coverage**: Generated unit tests across all layers — ViewModels, use cases, and repositories — growing the test suite from 5 to 12 test files.
- **Ports & Adapters Compliance**: Relocated adapter implementations (e.g., `RemoteFeatureToggleAdapter`) from the core module to `core:data` to maintain proper dependency direction.

### Workflow

All changes followed a **Think → Do** workflow:
1. **Think**: Discuss the finding, explore alternatives, and agree on an approach before writing any code.
2. **Do**: Implement the agreed-upon change as a minimal, focused commit.

Each fix was committed independently with a descriptive message.
