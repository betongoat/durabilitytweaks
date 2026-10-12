pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Picks the right Loom variant: remapping (Mojang mappings) for 1.21.x, plain Loom for unobfuscated 26.x
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // One jar per group of binary-compatible Minecraft releases.
        // The exact range each jar supports is set in stonecutter.properties.toml (mod.mc_compat).
        versions(
            "1.21.1",  // 1.21 - 1.21.1
            "1.21.3",  // 1.21.2 - 1.21.3
            "1.21.4",
            "1.21.5",
            "1.21.8",  // 1.21.6 - 1.21.8
            "1.21.10", // 1.21.9 - 1.21.10
            "1.21.11",
            "26.1.2",  // 26.1 - 26.1.2
            "26.2",
            "26.3",
        )
        vcsVersion = "26.3"
    }
}

rootProject.name = "DurabilityTweaks"
