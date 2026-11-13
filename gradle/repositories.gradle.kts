// Credentials belong in user Gradle properties or environment variables, never source control.
repositories {
    if (providers.gradleProperty("useMavenLocal").orNull == "true") mavenLocal()
    mavenCentral()
    for (apiName in listOf("model", "import", "export", "printer", "slicer")) {
        maven {
            name = "github" + apiName.replaceFirstChar { it.uppercase() }
            url = uri("https://maven.pkg.github.com/voxel-brew/api-$apiName")
            mavenContent { snapshotsOnly() }
            content { includeModule("dev.voxelbrew.api", apiName) }
            credentials {
                username =
                    providers
                        .gradleProperty("githubPackagesUser")
                        .orElse(providers.environmentVariable("GITHUB_PACKAGES_USER"))
                        .orNull
                password =
                    providers
                        .gradleProperty("githubPackagesToken")
                        .orElse(providers.environmentVariable("GITHUB_PACKAGES_TOKEN"))
                        .orNull
            }
        }
    }
}
