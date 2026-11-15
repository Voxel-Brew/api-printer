# Repository Guidelines

## Project Structure & Module Organization

This repository contains the Java 21 public printer API. Production contracts live in
`src/main/java/dev/voxelbrew/printer/api`; keep implementation, transport, and UI concerns out of
this module. Version metadata is in `src/main/resources`. Unit tests use the intentionally custom
`src/test/test/java` tree. Add integration and end-to-end tests under
`src/test/integrationTest/java` and `src/test/e2eTest/java`. Gradle build logic is split between
`build.gradle.kts` and `gradle/*.gradle.kts`; quality configuration lives in `config/quality`.

## Build, Test, and Development Commands

- `./gradlew check` — run unit and integration tests, formatting checks, SpotBugs, API dependency
  validation, and release-script tests.
- `./gradlew test` — run the JUnit 5 unit suite only.
- `./gradlew integrationTest` or `./gradlew e2eTest` — run the named custom suite; `e2eTest` is not
  included in `check`.
- `./gradlew spotlessApply` — format Java, Kotlin Gradle scripts, Markdown, YAML, and text files.
- `./gradlew publishToMavenLocal` — publish a local development artifact. Use
  `-PuseMavenLocal=true` when resolving locally published VoxelBrew dependencies.

Use the committed wrapper (`gradlew.bat` on Windows) and a local JDK 21; automatic JDK download is
disabled.

## Coding Style & Naming Conventions

Spotless enforces Palantir Java Format and ktfmt; do not hand-align formatted code. Java compilation
uses UTF-8, `-Xlint:all`, and treats warnings as errors. Follow standard Java naming: packages are
lowercase, types use `UpperCamelCase`, methods and fields use `lowerCamelCase`, and constants use
`UPPER_SNAKE_CASE`. Keep public packages under `dev.voxelbrew.printer.api`. Lombok is permitted only
as a compile-time source generator.

## Testing Guidelines

Write JUnit Jupiter tests with descriptive behavior names such as
`deviceAddressesAreValidatedBeforeSaving`. Name test classes `*Test` and keep their package aligned
with the code under test. Add regression coverage for contract changes and boundary validation. No
numeric coverage threshold is configured; prioritize observable public-API behavior.

## Commit & Pull Request Guidelines

Recent history favors short, imperative subjects, sometimes scoped (`build:`, `ci:`, `docs:`) or
milestone-prefixed (`M4:`). Keep each commit focused. Pull requests should explain the API impact,
link relevant issues, identify compatibility or publication changes, and list verification commands.
Screenshots are needed only when documentation rendering changes. Run `./gradlew check` before
requesting review.

## Security & Publication

Never commit tokens, signing keys, generated build output, or Gradle user properties. Preserve BOM
alignment and the rule that this API depends only on the VoxelBrew BOM and `api:model`. Do not
publish, tag releases, or change release versions merely to validate a patch.
