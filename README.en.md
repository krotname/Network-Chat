# Network Chat

[![CI](https://github.com/krotname/JavaNetworkChat/actions/workflows/ci.yml/badge.svg)](https://github.com/krotname/JavaNetworkChat/actions/workflows/ci.yml)
[![CodeQL](https://github.com/krotname/JavaNetworkChat/actions/workflows/codeql.yml/badge.svg)](https://github.com/krotname/JavaNetworkChat/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/krotname/JavaNetworkChat/badge)](https://securityscorecards.dev/viewer/?uri=github.com/krotname/JavaNetworkChat)
[![coverage summary](https://img.shields.io/badge/coverage-CI%20summary-blue)](https://github.com/krotname/JavaNetworkChat/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-007396)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)

## What this project demonstrates

Network Chat is a Java 21 chat application over TCP sockets with:

- a resilient server with user handshake and broadcast.
- a console client.
- a bot client with date/time commands.
- a Swing GUI client using MVC style structure.
- a production-like project layout with Gradle, tests, and CI.

![Swing GUI client](docs/images/gui-client.svg)

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

The default server port is `1500`. Programmatic server startup can use `ChatServerConfig` to set the
port, maximum client count, handshake timeout, and post-handshake read timeout.

## Architecture and protocol

The compact architecture contract is documented in [docs/architecture.md](docs/architecture.md).

- `ChatServer` accepts TCP connections and handles clients in a bounded executor.
- `ChatConnection` reads and writes one-line UTF-8 JSON frames.
- `ChatProtocol` serializes `ChatMessage`.
- For `TEXT` messages, `data` contains only raw text and `sender` contains the author.
- Console and Swing clients format display text such as `alice: hello`.
- The bot client reads date/time commands from `data` and uses the author from `sender`.

## Project structure

- `src/main/java` — core application classes.
- `src/test/java` — unit tests.
- `src/integrationTest/java` — protocol and network integration tests.
- `src/uiTest/java` — Swing smoke tests.
- `docs` — architecture notes and visual assets.
- `.github/workflows` — CI, CodeQL and Scorecard workflows.

## Testing

- `./gradlew test`
- `./gradlew integrationTest`
- `./gradlew uiTest`
- `./gradlew check`
- `./gradlew jacocoTestCoverageVerification`
- `./gradlew jacocoAllReport` (CI artifact source)

Coverage thresholds are enforced in Gradle and CI. The HTML JaCoCo report is uploaded as a CI
artifact, and line/branch coverage is published to the GitHub Actions Summary for the Linux job.

### Test strategy

- **Unit tests** (`src/test/java`) check protocol, bot command handling, and UI model invariants.
- **Integration tests** (`src/integrationTest/java`) exercise full server/client socket flow, handshake
  failures, resource limits, timeouts, and multiple peers.
- **UI smoke tests** (`src/uiTest/java`) verify Swing state rendering.
- **Future hardening tests**: contract validation and error-handling matrix can be added in the same
  structure.

## Quality gates

The repository runs:

- `checkstyle` for style and API cleanliness,
- `spotless` for deterministic formatting,
- `spotbugs` for bug-pattern analysis,
- `jaCoCo` line/branch coverage gate on core network/protocol layers (`80%/65%`),
- GitHub Actions pipeline on Linux + Windows,
- grouped dependency and workflow update signals via Dependabot,
- CodeQL and OpenSSF Scorecard security scans.

The quality surface is intentionally structured for a public review: clean `main` surface, automated checks,
and explicit licensing/operating docs.

## Language

- English project documentation in this file.
- Russian project documentation in `README.md`.

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

## Troubleshooting

- `Address already in use`: run the server on another port, for example `./gradlew runServer --args="--port 1600"`.
- GUI does not render in CI: UI smoke tests skip automatically in headless environments.
- Client disconnects immediately: check username uniqueness and nickname length (`3..64`, letters, digits, `_`, `-`).
- Client receives `Server is busy`: the configured `ChatServerConfig.maxClients` limit has been reached.

## Roadmap

- v1.1.x: stabilize protocol/server lifecycle, expand negative tests, and improve documentation.
- Later: rooms, message history, TLS, and persistent accounts as separate product-focused phases.
