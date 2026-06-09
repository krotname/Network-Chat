# Network Chat

[![CI](https://github.com/krotname/Network-Chat/actions/workflows/ci.yml/badge.svg)](https://github.com/krotname/Network-Chat/actions/workflows/ci.yml)
[![CodeQL](https://github.com/krotname/Network-Chat/actions/workflows/codeql.yml/badge.svg)](https://github.com/krotname/Network-Chat/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/krotname/Network-Chat/badge)](https://securityscorecards.dev/viewer/?uri=github.com/krotname/Network-Chat)
[![coverage](https://img.shields.io/badge/coverage-70%2B-green)](https://github.com/krotname/Network-Chat/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-007396)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)

## What this project demonstrates

Network Chat is a Java 21 chat application over TCP sockets with:

- a resilient server with user handshake and broadcast.
- a console client.
- a bot client with date/time commands.
- a Swing GUI client using MVC style structure.
- a production-like project layout with Gradle, tests, and CI.

## Run locally

### Prerequisites

- Java 21+

### Commands

```bash
./gradlew runServer --args="--port 1500"
./gradlew runClient
./gradlew runBotClient
./gradlew runGuiClient
```

Server and clients can also be run directly with Java by building jars from Gradle.

## Project structure

- `src/main/java` — core application classes.
- `src/test/java` — unit tests.
- `src/integrationTest/java` — protocol and network integration tests.
- `src/uiTest/java` — Swing smoke tests.
- `.github/workflows` — CI, CodeQL and Scorecard workflows.

## Testing

- `./gradlew test`
- `./gradlew integrationTest`
- `./gradlew uiTest`
- `./gradlew check`
- `./gradlew jacocoTestCoverageVerification`
- `./gradlew jacocoAllReport` (CI artifact source)

Coverage thresholds are enforced in Gradle and CI.

### Test strategy

- **Unit tests** (`src/test/java`) check protocol and UI model invariants.
- **Integration tests** (`src/integrationTest/java`) exercise full server/client socket flow with multiple peers.
- **UI smoke tests** (`src/uiTest/java`) verify Swing state rendering.
- **Future hardening tests**: contract validation and error-handling matrix can be added in the same
  structure.

## Quality gates

The repository runs:

- `checkstyle` for style and API cleanliness,
- `spotless` for deterministic formatting,
- `spotbugs` for bug-pattern analysis,
- `jaCoCo` line/branch coverage gate on core network/protocol layers (`70%/55%`),
- GitHub Actions pipeline on Linux + Windows,
- dependency and workflow update signals via Dependabot,
- CodeQL and OpenSSF Scorecard security scans.

The quality surface is intentionally structured for a public review: clean `main` surface, automated checks,
and explicit licensing/operating docs.

## Language

- English project documentation in this file.
- Russian project documentation in `Readme.md`.

## Why this repository is designed for review

This repository is organized to be review-friendly:

- explicit package structure,
- reproducible build and command surface (`./gradlew`),
- consistent protocol encoding,
- targeted comments in non-trivial methods,
- automated quality gates.

## Additional review signals

- Linux + Windows CI matrix for broader platform evidence.
- Automated quality gates via Checkstyle, Spotless, SpotBugs and JaCoCo.
- Security checks via CodeQL and OpenSSF Scorecard.
- Dependency and workflow automation via Dependabot.
- Explicit contributor and security docs.
