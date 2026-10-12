plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"

    replacements {
        // Mojang renamed ResourceLocation to Identifier in 1.21.11
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Universal jar: one file for every supported Minecraft version.
// It nests the per-version jars (META-INF/jars) and Fabric Loader picks the one whose
// "minecraft" and "java" requirements match the running game.
// `./gradlew universalJar` builds everything; add `-PskipBuild` to only pack jars already in build/libs/<version>/.

val modVersion = property("mod.version") as String
val versionJars = layout.buildDirectory.dir("libs/$modVersion")
val universalMeta = layout.buildDirectory.dir("universal")

val universalModJson = tasks.register("universalModJson") {
    if (!providers.gradleProperty("skipBuild").isPresent) {
        dependsOn(subprojects.map { "${it.path}:buildAndCollect" })
    }
    val jarsDir = versionJars
    val outputDir = universalMeta
    val version = modVersion
    outputs.dir(outputDir)
    outputs.upToDateWhen { false }
    doLast {
        val jars = jarsDir.get().asFile.listFiles { f -> f.name.endsWith(".jar") && !f.name.endsWith("-sources.jar") }
            ?.sortedBy { it.name }.orEmpty()
        check(jars.isNotEmpty()) { "No version jars found in ${jarsDir.get().asFile}" }
        val nested = jars.joinToString(",\n") { "    {\"file\": \"META-INF/jars/${it.name}\"}" }
        val json = """
            |{
            |  "schemaVersion": 1,
            |  "id": "durabilitytweaks-universal",
            |  "version": "$version",
            |  "name": "Durability Tweaks",
            |  "description": "Dura pack as a mod: worn armor changes colour as it loses durability. This jar contains every supported Minecraft version (1.21 - 26.3) and loads the matching one.",
            |  "authors": ["betongoat"],
            |  "contact": {
            |    "sources": "https://github.com/betongoat/durabilitytweaks",
            |    "issues": "https://github.com/betongoat/durabilitytweaks/issues"
            |  },
            |  "license": "MIT",
            |  "icon": "assets/durabilitytweaks/icon.png",
            |  "environment": "client",
            |  "depends": {
            |    "fabricloader": ">=0.16.0"
            |  },
            |  "jars": [
            |$nested
            |  ]
            |}
            |""".trimMargin()
        outputDir.get().asFile.mkdirs()
        outputDir.get().file("fabric.mod.json").asFile.writeText(json)
    }
}

tasks.register<Jar>("universalJar") {
    group = "build"
    description = "Packs all version jars into build/libs/durabilitytweaks-<version>.jar"
    dependsOn(universalModJson)
    archiveFileName = "durabilitytweaks-$modVersion.jar"
    destinationDirectory = layout.buildDirectory.dir("libs")
    from(universalMeta)
    from(file("src/main/resources/assets/durabilitytweaks/icon.png")) { into("assets/durabilitytweaks") }
    from(file("LICENSE")) { rename { "LICENSE_durabilitytweaks" } }
    from(versionJars) {
        include("*.jar")
        exclude("*-sources.jar")
        into("META-INF/jars")
    }
}
