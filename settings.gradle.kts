pluginManagement {
    val bomDirectory =
        providers.environmentVariable("VOXELBREW_BOM_DIR").orNull?.let { file(it) }
            ?: listOf("../../BOM", "../dependencies/bom", ".tools/bom")
                .map { file(it) }
                .firstOrNull { it.resolve("spotbugs-conventions").isDirectory }
    require(bomDirectory?.resolve("spotbugs-conventions")?.isDirectory == true) {
        "Set VOXELBREW_BOM_DIR to a VoxelBrew BOM checkout with SpotBugs conventions."
    }
    includeBuild(checkNotNull(bomDirectory).resolve("spotbugs-conventions"))
}

rootProject.name = "printer"
