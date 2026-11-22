import java.util.Properties

plugins {
    `java-library`
    `maven-publish`
    id("com.vanniktech.maven.publish.base") version "0.37.0"
    id("com.diffplug.spotless") version "8.11.0"
}

val qualityPolicy = "https://raw.githubusercontent.com/Voxel-Brew/BOM/master/quality"

apply(from = "$qualityPolicy/analysis.gradle")

apply(from = "$qualityPolicy/binary-compatibility.gradle")

group = "dev.voxelbrew.api"

version = providers.gradleProperty("projectVersion").get()

apply(from = "gradle/repositories.gradle.kts")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

apply(from = "gradle/lombok.gradle.kts")

dependencies {
    api(platform("dev.voxelbrew:bom:${providers.gradleProperty("bomVersion").get()}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets.test {
    java.setSrcDirs(listOf("src/test/test/java"))
    resources.setSrcDirs(listOf("src/test/test/resources"))
}

for (suite in listOf("integrationTest", "e2eTest")) {
    val source =
        sourceSets.create(suite) {
            java.setSrcDirs(listOf("src/test/$suite/java"))
            resources.setSrcDirs(listOf("src/test/$suite/resources"))
            compileClasspath += sourceSets.main.get().output
            runtimeClasspath += sourceSets.main.get().output
        }
    configurations[source.implementationConfigurationName].extendsFrom(
        configurations.testImplementation.get()
    )
    configurations[source.runtimeOnlyConfigurationName].extendsFrom(
        configurations.testRuntimeOnly.get()
    )
    tasks.register<Test>(suite) {
        testClassesDirs = source.output.classesDirs
        classpath = source.runtimeClasspath
        shouldRunAfter(tasks.test)
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

val checkJavaMemberOrder by
    tasks.registering(JavaExec::class) {
        group = "verification"
        description = "Check Java member declaration order."
        javaLauncher.set(
            project.extensions.getByType<JavaToolchainService>().launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        )
        val checker = resources.text.fromUri("$qualityPolicy/JavaMemberOrder.java")
        jvmArgs("--source", "21")
        mainClass.set(providers.provider { checker.asFile().absolutePath })
        args("src", "gradle")
    }

tasks.named("check") { dependsOn(checkJavaMemberOrder) }

val formatterPolicy =
    Properties().apply {
        resources.text.fromUri("$qualityPolicy/spotless.properties").asReader().use { load(it) }
    }

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat(formatterPolicy.getProperty("palantirJavaFormat"))
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "gradle/*.gradle.kts")
        ktfmt(formatterPolicy.getProperty("ktfmt")).kotlinlangStyle()
        endWithNewline()
    }
    format("docs") {
        target("*.md", ".github/**/*.yml")
        prettier(formatterPolicy.getProperty("prettier"))
            .config(
                mapOf(
                    "printWidth" to formatterPolicy.getProperty("printWidth").toInt(),
                    "proseWrap" to formatterPolicy.getProperty("proseWrap"),
                )
            )
        endWithNewline()
    }
    format("text") {
        target("*.properties", ".gitignore", ".gitattributes", "lombok.config")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<Jar>().configureEach { from("LICENSE") { into("META-INF") } }

val buildVersion = version.toString()

tasks.jar { manifest.attributes("Implementation-Version" to buildVersion) }

tasks.processResources {
    inputs.property("version", buildVersion)
    filesMatching("voxelbrew-*-api-version.properties") { expand("version" to buildVersion) }
}

val verifyApiDependencies by
    tasks.registering {
        doLast {
            for (scope in listOf("compileClasspath", "runtimeClasspath")) {
                configurations[scope].incoming.resolutionResult.allComponents.forEach {
                    val group = it.moduleVersion?.group
                    check(group != "dev.voxelbrew" || it.moduleVersion?.name == "bom") {
                        "API must not depend on application modules: ${it.id}"
                    }
                }
            }
        }
    }

tasks.check { dependsOn("integrationTest", verifyApiDependencies) }

publishing {
    publications {
        create<MavenPublication>("api") {
            from(components["java"])
            pom {
                name.set("VoxelBrew " + project.name + " API")
                description.set(
                    "Public " + project.name + " contracts for VoxelBrew CAD and 3D printing."
                )
                url.set("https://github.com/Voxel-Brew/api-" + project.name)
                developers {
                    developer {
                        id.set("voxelbrew")
                        name.set("VoxelBrew contributors")
                        url.set("https://github.com/Voxel-Brew")
                    }
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("https://github.com/Voxel-Brew/api-" + project.name)
                    connection.set(
                        "scm:git:https://github.com/Voxel-Brew/api-" + project.name + ".git"
                    )
                    developerConnection.set(
                        "scm:git:ssh://git@github.com/Voxel-Brew/api-" + project.name + ".git"
                    )
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/voxel-brew/api-" + project.name)
            credentials {
                username =
                    providers
                        .environmentVariable("GITHUB_ACTOR")
                        .orElse(providers.gradleProperty("githubPackagesUser"))
                        .orNull
                password =
                    providers
                        .environmentVariable("GITHUB_TOKEN")
                        .orElse(providers.gradleProperty("githubPackagesToken"))
                        .orNull
            }
        }
    }
}

dependencies {
    api("dev.voxelbrew.api:model")
    implementation("org.slf4j:slf4j-api")
}

// Opt-in signing keeps GitHub snapshots and local development credential-free.
val centralRelease = providers.gradleProperty("centralRelease").orNull == "true"

mavenPublishing {
    publishToMavenCentral(
        validateDeployment = com.vanniktech.maven.publish.DeploymentValidation.PUBLISHED
    )
    if (centralRelease) {
        project.extra["signing.keyId"] =
            providers.environmentVariable("SIGNING_KEY_ID").orNull ?: ""
        project.extra["signing.password"] =
            providers.environmentVariable("SIGNING_PASSWORD").orNull ?: ""
        project.extra["signing.secretKeyRingFile"] =
            providers.environmentVariable("SIGNING_KEY_RING").orNull ?: ""
        signAllPublications()
    }
}

val verifyRelease by
    tasks.registering {
        doLast {
            check(Regex("[0-9]+\\.[0-9]+\\.[0-9]+").matches(buildVersion)) {
                "Maven Central releases require projectVersion=A.B.C."
            }
            for (scope in listOf("compileClasspath", "runtimeClasspath")) {
                configurations[scope].allDependencies.forEach {
                    val dependencyVersion = it.version
                    check(
                        dependencyVersion == null ||
                            (!dependencyVersion.contains("SNAPSHOT", ignoreCase = true) &&
                                !dependencyVersion.contains("+") &&
                                !dependencyVersion.startsWith("latest.") &&
                                !dependencyVersion.contains("[") &&
                                !dependencyVersion.contains("("))
                    ) {
                        "Release dependency must have a fixed release version: ${it.group}:${it.name}:$dependencyVersion"
                    }
                }
            }
        }
    }

tasks
    .matching { it.name.contains("MavenCentral") }
    .configureEach {
        dependsOn(verifyRelease)
        doFirst {
            check(centralRelease) {
                "Central publishing requires -PcentralRelease=true and signing credentials."
            }
        }
    }

val releaseScriptTest by
    tasks.registering(Exec::class) {
        commandLine("node", "--test", ".github/scripts/release.test.cjs")
    }

tasks.check { dependsOn(releaseScriptTest) }
