import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.SpotBugsTask

plugins {
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "8.11.0"
    id("com.github.spotbugs") version "6.5.13"
}

group = "dev.voxelbrew.api"

version = providers.gradleProperty("projectVersion").get()

apply(from = "gradle/repositories.gradle.kts")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

apply(from = "gradle/lombok.gradle.kts")

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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

spotbugs {
    toolVersion.set("4.10.5")
    reportLevel.set(Confidence.HIGH)
    includeFilter.set(file("config/quality/spotbugs-include.xml"))
}

tasks.withType<SpotBugsTask>().configureEach {
    enabled = name == "spotbugsMain"
    reports.create("html") { required.set(true) }
}

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat("2.103.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "gradle/*.gradle.kts")
        ktfmt("0.59").kotlinlangStyle()
        endWithNewline()
    }
    format("docs") {
        target("*.md", ".github/**/*.yml")
        prettier("3.6.2").config(mapOf("printWidth" to 100, "proseWrap" to "preserve"))
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
                    check(group != "dev.voxelbrew") {
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
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit")
                        distribution.set("repo")
                    }
                }
                scm { url.set("https://github.com/Voxel-Brew/api-" + project.name) }
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

dependencies { api("dev.voxelbrew.api:model:${providers.gradleProperty("modelVersion").get()}") }
