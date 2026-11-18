# VoxelBrew Printer API

[![Build](https://github.com/Voxel-Brew/api-printer/actions/workflows/check.yml/badge.svg?branch=master)](https://github.com/Voxel-Brew/api-printer/actions/workflows/check.yml?query=branch%3Amaster)
[![Maven Central](https://img.shields.io/maven-central/v/dev.voxelbrew.api/printer?label=Maven%20Central)](https://central.sonatype.com/artifact/dev.voxelbrew.api/printer)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Java 21 API for reusable printer drivers, typed connection settings, and hardware
profiles. Integrate your printer with both the VoxelBrew desktop application and CLI.

## Main types

| Type                  | Purpose                                                                          |
| --------------------- | -------------------------------------------------------------------------------- |
| `Driver`              | Submit print artifacts and read printer status using connection settings         |
| `DriverGui`           | Describe configuration controls, validate input, and build typed settings        |
| `DriverConfiguration` | Hold immutable connection settings with defensive password copies                |
| `Printer`             | Save a named device, its driver configuration, and a hardware profile snapshot   |
| `Profile`             | Describe the bed, excluded zones, tools, dimensions, firmware, and output format |
| `DriverRegistry`      | Discover drivers from the classpath and installed plugin JARs                    |

The GUI and CLI share configuration forms and validation. Desktop printer operations
run on background workers. Submission success means acceptance, not print completion;
uncertain submissions must not be retried automatically. Documents retain hardware
settings and target IDs, never driver credentials.

This MIT-licensed repository contains public contracts and value types. Built-in
drivers, network transports, and file stores live in the application's `printer-impl` module.

## Use the API

Compile your plugin against the API version matching its target VoxelBrew host:

```kotlin
dependencies {
    compileOnly("dev.voxelbrew.api:printer:<version>")
}
```

Register your driver through `META-INF/services`. Hosts obtain `DriverRegistry`
through `ServiceLoader` and own its lifetime.

[Write your own driver](docs/DEV_README.md) for project setup, complete code examples,
configuration forms, plugin registration, and testing guidance.

## Build

Use Java 21 and the checked-in wrapper: `./gradlew check` on Linux/macOS or
`.\gradlew.bat check` on Windows. For coordinated API changes, use the VoxelBrew
workspace composite described in the developer guide.
