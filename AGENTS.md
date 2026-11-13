# Printer API

- Own headless device and job-transfer contracts under MIT.
- Depend only on api/model. Keep transport libraries and UI types private to implementations.
- Device identities are distinct from slicer printer presets.

## Standalone repository

- Use Java 21 and the committed Gradle wrapper. Preserve the MIT license and public Java packages.
- Run `./gradlew spotlessApply` (Windows: `.\gradlew.bat spotlessApply`) before every commit, then review changes.
- Run `check` and validate Maven POM dependencies when changing publication configuration.
- Never add application-module dependencies or commit generated output or credentials.
