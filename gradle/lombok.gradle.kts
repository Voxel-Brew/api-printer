// Lombok is a source-generation tool, never an application runtime dependency.

val dependencyBom = "dev.voxelbrew:bom:${providers.gradleProperty("bomVersion").get()}"

extensions.getByType<SourceSetContainer>().configureEach {
    // Processor paths and custom source sets do not inherit the main API platform.
    dependencies.add(compileOnlyConfigurationName, dependencies.platform(dependencyBom))
    dependencies.add(annotationProcessorConfigurationName, dependencies.platform(dependencyBom))
    dependencies.add(compileOnlyConfigurationName, "org.projectlombok:lombok")
    dependencies.add(annotationProcessorConfigurationName, "org.projectlombok:lombok")
}

dependencies {
    add("implementation", "org.slf4j:slf4j-api")
    add("testRuntimeOnly", "org.slf4j:slf4j-jdk14")
    // Applications choose the backend; published libraries leave that choice to their host.
    if (project.name in listOf("cli", "gui")) {
        add("runtimeOnly", "org.slf4j:slf4j-jdk14")
    }
}

// Cooperating Lombok/Dagger processors intentionally leave other annotations unclaimed.
tasks.withType<JavaCompile>().configureEach {
    inputs.file(rootProject.file("lombok.config")).withPathSensitivity(PathSensitivity.RELATIVE)
    options.compilerArgs.add("-Xlint:-processing")
}
