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

## Maven Central releases

Release coordinates are `dev.voxelbrew.api:ARTIFACT:VERSION`, under the verified
`dev.voxelbrew` namespace. Snapshots remain in GitHub Packages; releases resolve
from Maven Central without GitHub credentials. Local `publishToMavenLocal` remains
available without signing credentials.

### GitHub Actions

1. Run **Prepare release** (`release-prepare.yml`) on `master`. It creates
   `release/A.B.x`, pins API dependencies to exact releases, and advances master
   to the next minor snapshot using an atomic push. Optional `model-version` and
   `export-version` inputs override dependency versions. Otherwise their current
   version without `-SNAPSHOT` is used; that release must exist in Central before
   publishing this API.
2. Run **Publish to prod** (`release.yml`) on that release branch with the default
   `dry-run: true`. This checks and builds a local release candidate without
   changing remote refs or publishing. It resolves real released dependencies,
   not composite source substitutions.
3. Run again with `dry-run: false`. The workflow validates signing credentials,
   signs the candidate, reserves immutable tag `vA.B.C`, and publishes to the
   Central Portal. Only after Central reports `PUBLISHED` does it create the
   GitHub release and advance the release branch to the next patch snapshot.

For the first API release, publish **model**, then **export/import/printer**, then
**slicer**. Slicer depends on model and export. Allow Central's public repository
to propagate each dependency before releasing its consumers. API versions remain
independent of application versions.

Configure these repository or organization secrets (grant organization secrets
access to each API repository):

| Secret                    | Value                                                         |
| ------------------------- | ------------------------------------------------------------- |
| `MAVEN_CENTRAL_USERNAME`  | Central Portal token username, not account login              |
| `MAVEN_CENTRAL_PASSWORD`  | Central Portal token password                                 |
| `SIGNING_KEY_ID`          | Signing key ID (short ID for Gradle's legacy key-ring reader) |
| `SIGNING_KEY_RING_BASE64` | Base64-encoded exported secret key ring                       |
| `SIGNING_PASSWORD`        | Signing key passphrase; omit for an unprotected key           |

The public signing key must be distributed as required by Central and must not
be expired or revoked. A Portal token authenticates uploads; it does not replace
the required PGP signatures. Keys are decoded only into the temporary runner
directory and removed in an always-run cleanup step. No secrets belong in Git.

The build supplies binary, sources and Javadoc JARs, checksums, signatures and POM
metadata (license, developers and SCM). It rejects snapshots and dynamic direct
dependencies for Central releases. `-PcentralRelease=true` explicitly enables
signing and Central upload. For local publishing, use `SIGNING_KEY_ID`,
`SIGNING_PASSWORD`, `SIGNING_KEY_RING` (file path), and Gradle environment variables
`ORG_GRADLE_PROJECT_mavenCentralUsername` / `ORG_GRADLE_PROJECT_mavenCentralPassword`.

### Recovery

If signing or verification fails, no release tag is pushed. If an upload fails,
the reserved tag remains and the branch stays at its original snapshot. Rerun
on the unchanged branch to reuse exactly that tag's candidate; a different tree
or source commit is rejected. Never delete or overwrite a published version.

If Central already published but the workflow failed before advancing the branch,
do not upload again. Check out the same source, run `node .github/scripts/release.cjs
candidate` with the original workflow's `GITHUB_REF_NAME`, `GITHUB_SHA` and
`GITHUB_REPOSITORY`, verify the existing Central release, then run `node
.github/scripts/release.cjs finish` with an authorized `GH_TOKEN`. This final step
creates the GitHub release if absent and advances the branch. Port release fixes
back to master through normal reviewed changes; do not merge release version
bumps into master.

References: [Central requirements](https://central.sonatype.org/publish/requirements/)
and [Gradle Central publishing plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/).
