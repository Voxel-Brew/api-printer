# Repository Guidelines

## Project Structure & Module Organization

This repository contains the Java 21 public printer API. Production contracts live in
`src/main/java/dev/voxelbrew/api/printer`; keep implementation, transport, and UI concerns out of
this module. Version metadata is in `src/main/resources`. Unit tests use the intentionally custom
`src/test/test/java` tree. Add integration and end-to-end tests under
`src/test/integrationTest/java` and `src/test/e2eTest/java`. Gradle build logic is split between
`build.gradle.kts` and `gradle/*.gradle.kts`; SpotBugs configuration comes from the public BOM master branch.

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

Run `./gradlew checkBinaryCompatibility` to compare the built JAR with the latest stable Maven
Central release in the same major at or below `projectVersion` (ignoring `-SNAPSHOT`). The
`check` task includes this comparison on pull requests and pushes to master and release branches.

## Coding Style & Naming Conventions

Spotless enforces Palantir Java Format and ktfmt; do not hand-align formatted code. Java compilation
uses UTF-8, `-Xlint:all`, and treats warnings as errors. Follow standard Java naming: packages are
lowercase, types use `UpperCamelCase`, methods and fields use `lowerCamelCase`, and constants use
`UPPER_SNAKE_CASE`. Keep public packages under `dev.voxelbrew.api.printer`. Lombok is permitted only
as a compile-time source generator.

Run `./gradlew checkJavaMemberOrder` to check Java source declaration order. The `check` and
`build` tasks include it. Within each class, start with static final fields, then other static fields,
final instance fields, other instance fields, static nested types, inner classes, static methods,
constructors, and instance methods. Preserve Java initialization order when moving fields.

## Testing Guidelines

Write JUnit Jupiter tests with descriptive behavior names such as
`deviceAddressesAreValidatedBeforeSaving`. Name test classes `*Test` and keep their package aligned
with the code under test. Add regression coverage for contract changes and boundary validation. No
numeric coverage threshold is configured; prioritize observable public-API behavior.

## Pre-commit SpotBugs

The build resolves VoxelBrew artifacts through Maven and fetches shared quality policy
from the public BOM master branch.

Before committing, run `./gradlew spotbugsMain` from this API repository or
`./gradlew :printer:spotbugsMain` from the composite workspace.
Use `.\gradlew.bat` in place of `./gradlew` on Windows.

## Commit & Pull Request Guidelines

Prefer **Rebase and merge** for pull requests. Use **Squash and merge** only as an exception.
The **Create a merge commit** option is blocked.

Recent history favors short, imperative subjects, sometimes scoped (`build:`, `ci:`, `docs:`) or
milestone-prefixed (`M4:`). Keep each commit focused. Pull requests should explain the API impact,
link relevant issues, identify compatibility or publication changes, and list verification commands.
Screenshots are needed only when documentation rendering changes. Run `./gradlew check` before
requesting review.

When a commit addresses a known GitHub issue, include the issue number in the
commit subject as the scope, for example `feat(#1337): foo boo bar baz`.

When a pull request resolves a known GitHub issue, add `Closes #123` to the PR description,
using the actual issue number. For an issue in another repository, use
`Closes owner/repository#123`. Add the closing reference to the PR targeting
`master` so GitHub closes the issue when the PR is merged.

## Security & Publication

Never commit tokens, signing keys, generated build output, or Gradle user properties. Preserve BOM
alignment and the rule that this API depends only on the VoxelBrew BOM and `api:model`. Do not
publish, tag releases, or change release versions merely to validate a patch.

## Lombok in Java code

Use Lombok where it removes routine Java boilerplate without obscuring a public contract. Apply these annotations in Java-bearing repositories and modules; the website has no Java production code.

- `@Getter` / `@Setter`: Generate straightforward accessors for fields that should be exposed or mutable. Prefer field-level annotations when only some fields qualify; retain custom accessors that enforce invariants or make defensive copies.
- `@RequiredArgsConstructor` / `@NoArgsConstructor`: Generate constructors for required final or `@NonNull` fields, or a no-argument constructor when a framework or serializer needs one. Keep explicit constructors when they validate, transform, or document special initialization behavior.
- `@NonNull`: Mark required fields, parameters, and other supported elements to express and enforce non-null inputs. Replace equivalent `Objects.requireNonNull` checks in those places; keep explicit checks when they provide distinct behavior or messages.
- `@ToString`: Generate useful diagnostic string representations for value or state classes. Exclude secrets, large graphs, and recursive relationships.
- `@Data`: Use for mutable data holders that genuinely need getters, setters, equality, hashing, and string representation together. Prefer narrower annotations when only some behavior is appropriate.
- `@Value`: Use for immutable value types when its generated constructor, accessors, equality, hashing, and string representation fit the contract. Preserve defensive copies for mutable components.
- `@EqualsAndHashCode`: Generate value equality for classes whose identity is defined by selected fields. Choose included fields and superclass handling deliberately; avoid mutable fields that would break hash collections.
- `@Slf4j`: Add a logger to classes that emit operational diagnostics. Use parameterized logging and keep credentials and other sensitive data out of logs.
- `@Builder`: Use for objects with many optional or named construction inputs when it improves call-site clarity. Keep required-field validation and invariants explicit.
- `@UtilityClass`: Use for stateless collections of static helpers that must not be instantiated. Do not use it for classes that may need instances, inheritance, or dependency injection.

## AssertJ in Java tests

Use AssertJ (org.assertj:assertj-core) for assertions in Java unit, integration, and end-to-end tests. Keep JUnit Jupiter for test lifecycle, parameterization, and execution. Declare AssertJ without a version in test dependencies; the BOM manages its version. Prefer ssertThat, ssertThatThrownBy, and ssertThatCode with descriptive failure messages and appropriate numeric tolerances. Do not add new JUnit Assertions calls.

## Service injection

Resolve services by their interfaces at the wiring point and pass them to consumers through
constructors. Provider classes must not expose static service getters or downcast discovered
services to their implementations.

## Release workflows

Dispatch **Prepare release** on `master` to cut `release/A.B.x` and open the
next-minor snapshot pull request. Merge that PR through normal branch protection.
Dispatch `release.yml` on the release branch, first with the default dry run,
then with the dry run disabled to sign, reserve annotated tag `vA.B.C`, publish
to Maven Central, create the GitHub release and advance the patch snapshot.

Release-branch pushes and completed publications run `release-merge-back.yml`.
It proposes release fixes in a PR against master, retaining master's
`projectVersion`. Resolve other conflicts through normal review. To retry,
dispatch merge-back on the release branch.

Allow GitHub Actions to create pull requests, or configure `RELEASE_PR_TOKEN`
with contents and pull-request write access for the automation branches. Use a
dedicated token when branch protection requires CI triggered by PR updates.
