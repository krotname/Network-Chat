# Contributing

## Setup

1. Install Java 21.
2. `./gradlew test`.
3. Verify coverage gate with `./gradlew jacocoTestCoverageVerification`.

## Pull request checklist

- keep API changes small and explained in PR description;
- add or update unit/integration tests for changed behavior;
- include manual or screenshot evidence for UI changes when possible;
- ensure `gradle check` passes.

## Commit style

- `feat: ...`
- `fix: ...`
- `test: ...`
- `chore: ...`
