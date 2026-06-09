# Network Chat

[![CI](https://github.com/krotname/JavaNetworkChat/actions/workflows/ci.yml/badge.svg)](https://github.com/krotname/JavaNetworkChat/actions/workflows/ci.yml)
[![CodeQL](https://github.com/krotname/JavaNetworkChat/actions/workflows/codeql.yml/badge.svg)](https://github.com/krotname/JavaNetworkChat/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/krotname/JavaNetworkChat/badge)](https://securityscorecards.dev/viewer/?uri=github.com/krotname/JavaNetworkChat)
[![coverage](https://img.shields.io/badge/coverage-70%2B-green)](https://github.com/krotname/JavaNetworkChat/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-007396)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)

Документация на английском языке: [README.en.md](README.en.md).

## Что проект демонстрирует

Network Chat — это Java 21 приложение для сетевого чата поверх TCP сокетов с акцентом на качество разработки:

- сервер с handshake и рассылкой сообщений;
- консольный клиент;
- бот-клиент с командами времени/даты;
- GUI клиент на Swing с разделением на MVC;
- структуру продакшн-проекта с Gradle, тестами и CI.

## Запуск

```bash
./gradlew runServer --args="--port 1500"
./gradlew runClient
./gradlew runBotClient
./gradlew runGuiClient
```

## Тесты и качество

- `./gradlew test`
- `./gradlew integrationTest`
- `./gradlew uiTest`
- `./gradlew check`
- `./gradlew jacocoAllReport`
- `./gradlew jacocoTestCoverageVerification`

## Стратегия тестирования

- **Unit-тесты** (`src/test/java`) — протокол и модель GUI.
- **Интеграционные тесты** (`src/integrationTest/java`) — подключение нескольких клиентов к серверу и обмен сообщениями.
- **UI smoke тесты** (`src/uiTest/java`) — проверка отрисовки состояния окна чата.
- **План роста** — контракты протокола и матрицы негативных сценариев (подключение дубликатов, некорректные пакеты).

Для оценки покрытия используется JaCoCo: в CI порог для ядра (`network` + `protocol`) — `70%/55%` (`line`/`branch`).

## Структура репозитория

- `src/main/java` — код приложения.
- `src/test/java` — unit-тесты.
- `src/integrationTest/java` — интеграционные тесты.
- `src/uiTest/java` — smoke тесты UI.
- `.github/workflows` — CI и проверки безопасности.

## Дополнительные сигналы качества

- CI на Linux и Windows.
- Авто-проверки: Checkstyle, Spotless, SpotBugs, JaCoCo.
- Security проверки: CodeQL и OpenSSF Scorecard.
- Dependabot для обновлений зависимостей и Actions.
- Явно оформленные файлы `CONTRIBUTING.md` и `SECURITY.md`.
