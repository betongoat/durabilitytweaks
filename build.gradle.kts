plugins {
    // Applies the correct Loom variant for the current Minecraft version
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val mc = sc.current.parsed

val requiredJava: JavaVersion = when {
    mc >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// Armor rendering was rewritten in 1.21.2 (EquipmentLayerRenderer), so each side gets its own mixin.
val clientMixins: List<String> = buildList {
    add("MinecraftMixin")
    if (mc >= "1.21.2") add("EquipmentLayerRendererMixin") else add("HumanoidArmorLayerMixin")
}

val hasClientGameTests = mc >= "1.21.4"

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    // The whole Fabric API: the mod depends on "fabric-api", and the client game tests need several of its modules
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
}

if (hasClientGameTests) {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = "durabilitytweaks-gametest"
            enableGameTests = false
            enableClientGameTests = true
            eula = true
        }
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
    }

    // `-PsmokeTest`: runClient boots to the title screen and quits, used by CI on versions without client game tests
    if (providers.gradleProperty("smokeTest").isPresent) {
        runConfigs.named("client") {
            vmArg("-Ddurabilitytweaks.smokeTest=true")
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to sc.properties.get<String>("mod.version"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "java" to requiredJava.majorVersion,
            "loader" to Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value,
        )
        val mixinProps = mapOf(
            "java" to "JAVA_${requiredJava.majorVersion}",
            "client_mixins" to clientMixins.joinToString(", ") { "\"$it\"" },
        )
        inputs.properties(props)
        inputs.properties(mixinProps)

        filesMatching("fabric.mod.json") { expand(props) }
        filesMatching("*.mixins.json") { expand(mixinProps) }
    }

    withType<Jar> {
        // Compile-only Mod Menu API stubs: the real classes come from Mod Menu at runtime
        exclude("com/terraformersmc/**")

        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies them to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
    }
}
