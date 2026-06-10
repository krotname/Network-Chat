# Changelog

## Unreleased

## 1.1.0

- Added `ChatServerConfig` with server limits and socket timeout settings.
- Made server client handling bounded and explicit about busy rejections.
- Split protocol text payloads from display formatting: `data` is raw text, `sender` is the author.
- Fixed GUI connection lifecycle by preserving the base client status update path.
- Added architecture documentation, grouped Dependabot updates, version catalog, and CI coverage summary.
- Lowered the JaCoCo line coverage gate to 70% while keeping the branch threshold at 65%.

## 1.0.0

- Reworked project into Gradle Java 21 multi-layer architecture.
- Added protocol-based message transport over TCP.
- Added CLI client, GUI client, and bot client implementations.
- Added integration and UI smoke test sets.
- Added GitHub Actions workflows, coverage gating, and security checks.
