# VoxelBrew printer API

## Standalone build and publication

Java 21 and the committed Gradle wrapper are required. Run `./gradlew check`
(`.\gradlew.bat check` on Windows). This MIT library publishes as
`dev.voxelbrew.api:printer:0.1.0-SNAPSHOT`; Java package names remain unchanged.
Use `./gradlew publishToMavenLocal` for local publication. Consumers opt in with
`-PuseMavenLocal=true`. `publishApiPublicationToGitHubPackagesRepository` publishes remotely.

GitHub Packages reads use `GITHUB_PACKAGES_USER` and `GITHUB_PACKAGES_TOKEN`
(a classic token with read:packages), or user Gradle properties
`githubPackagesUser` and `githubPackagesToken`. Publishing uses `GITHUB_ACTOR`
and `GITHUB_TOKEN`. Never store credentials in this repository.

The local workspace at `D:\repos\VoxelBrew` includes this checkout at `api/printer`
and substitutes API dependencies with local sources. API versions are independent
of the application version. Pushes to master publish snapshots after checks;
pull requests only verify. Fork PRs use checked-out public API sources for dependencies.
