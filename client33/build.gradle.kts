import net.ltgt.gradle.errorprone.errorprone
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

plugins {
    alias(libs.plugins.fabric.loom)
    id("maven-publish")
    alias(libs.plugins.errorprone)
}

val archivesBaseName = providers.gradleProperty("archives_base_name").get()
val mavenGroup = providers.gradleProperty("maven_group").get()
val runErrorProne = providers.gradleProperty("errorprone").isPresent

base {
    archivesName = archivesBaseName
    group = mavenGroup

    val suffix = providers.gradleProperty("build_number").getOrElse("local")
    version = "${libs.versions.minecraft.get()}-$suffix"
}

repositories {
    maven {
        name = "upstream-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "upstream-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    maven {
        name = "ViaVersion"
        url = uri("https://repo.viaversion.com")
    }
    mavenCentral()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        content { includeGroup("org.inspirenxe") }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

val modInclude = configurations.create("modInclude")
val jij = configurations.create("jij")
val launcher = sourceSets.create("launcher") {
    java.srcDir("src/launcher/java")
}

configurations {
    // include mods
    implementation.configure {
        extendsFrom(modInclude)
    }
    include.configure {
        extendsFrom(modInclude)
    }

    // include libraries (jar-in-jar)
    implementation.configure {
        extendsFrom(jij)
    }
    include.configure {
        extendsFrom(jij)
    }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    val fapiVersion = libs.versions.fabric.api.get()
    modInclude("net.fabricmc.fabric-api:fabric-api:$fapiVersion")
    modInclude(fabricApi.module("fabric-api-base", fapiVersion))
    modInclude(fabricApi.module("fabric-resource-loader-v1", fapiVersion))

    // Compat fixes
    compileOnly(fabricApi.module("fabric-renderer-indigo", fapiVersion))
    compileOnly(libs.sodium) { isTransitive = false }
    compileOnly(libs.lithium) { isTransitive = false }
    compileOnly(libs.iris) { isTransitive = false }
    compileOnly(libs.viafabricplus) { isTransitive = false }
    compileOnly(libs.viafabricplus.api) { isTransitive = false }

    compileOnly(libs.baritone)
    compileOnly(libs.modmenu)

    // October 2026 Fabric release: records received chunks for extended terrain and ESP.
    modInclude("maven.modrinth:bobby:J7YnfCss") { isTransitive = false }

    // Strata includes its API, SPI and core as nested jars.
    modInclude("org.inspirenxe:strata-minecraft-fabric:0.1.0-20261027.015835-7")
    compileOnly("org.inspirenxe:strata-api:0.1.0-20261027.000854-11")

    // Libraries (JAR-in-JAR)
    // Minecraft moved to SDL; existing import/export dialogs still require tinyfd.
    jij("org.lwjgl:lwjgl-tinyfd:3.4.3") { isTransitive = false }
    listOf("linux", "linux-arm64", "windows", "windows-arm64", "macos", "macos-arm64").forEach { platform ->
        val nativeDialog = "org.lwjgl:lwjgl-tinyfd:3.4.3:natives-$platform"
        runtimeOnly(nativeDialog) { isTransitive = false }
        include(nativeDialog) { isTransitive = false }
    }
    jij("io.github.humbleui:skija-shared:0.143.18")
    listOf("linux-x64", "linux-arm64", "windows-x64", "macos-x64", "macos-arm64").forEach { platform ->
        jij("io.github.humbleui:skija-$platform:0.143.18") { isTransitive = false }
    }
    jij(libs.orbit)
    jij(libs.starscript)
    jij(libs.reflections)
    // Loom's include configuration does not automatically package transitive dependencies.
    // These versions match the runtime dependencies of Reflections and Skija.
    jij("org.javassist:javassist:3.28.0-GA") { isTransitive = false }
    jij("io.github.humbleui:types:0.2.0") { isTransitive = false }
    jij(libs.sqlite.jdbc) { isTransitive = false }

    // Error Prone
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }

    if (System.getenv("CI")?.toBoolean() == true) {
        withSourcesJar()
        withJavadocJar()
    }
}

// Handle transitive dependencies for jar-in-jar
// Based on implementation from BaseProject by florianreuth/EnZaXD
// Source: https://github.com/florianreuth/BaseProject/blob/main/src/main/kotlin/de/florianreuth/baseproject/Fabric.kt
// Licensed under Apache License 2.0
val jijExcluded = setOf("org.slf4j", "jsr305")
listOf("api", "implementation", "include").forEach { configName ->
    configurations.named(configName).configure {
        defaultDependencies {
            configurations.getByName("jij").incoming.resolutionResult.allComponents
                .mapNotNull { it.id as? ModuleComponentIdentifier }
                .forEach { id ->
                    val notation = "${id.group}:${id.module}:${id.version}"
                    if (jijExcluded.none { notation.contains(it) }) {
                        add(project.dependencies.create(notation) {
                            isTransitive = false
                        })
                    }
                }
        }
    }
}

loom {
    accessWidenerPath = file("src/main/resources/client33.classtweaker")
}

fun toMinecraftCompat(version: String): String {
    // Stable release
    val stable = Regex("""^(\d{2})\.([1-9]\d*)(?:\.(\d+))?$""")

    stable.matchEntire(version)?.let {
        val (year, drop, _) = it.destructured
        return "~$year.$drop"
    }

    // Prerelease
    val pre = Regex("""^(\d{2})\.([1-9]\d*)-pre[-.](\d+)$""")
    pre.matchEntire(version)?.let {
        return version.replace("-pre-", "-pre.")
    }

    // Release Candidate
    val rc = Regex("""^(\d{2})\.([1-9]\d*)-rc[-.](\d+)$""")
    rc.matchEntire(version)?.let {
        return version.replace("-rc-", "-rc.")
    }

    // fallback
    return version
}

tasks {
    processResources {
        val buildNumber = providers.gradleProperty("build_number").getOrElse("")
        val commit = providers.gradleProperty("commit").getOrElse("")

        val propertyMap = mapOf(
            "version" to project.version,
            "build_number" to buildNumber,
            "commit" to commit,
            "jdk_version" to libs.versions.jdk.get(),
            "minecraft_version" to toMinecraftCompat(libs.versions.minecraft.get()),
            "loader_version" to libs.versions.fabric.loader.get()
        )

        inputs.properties(propertyMap)
        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    // Compile launcher with Java 8 for backwards compatibility
    named<JavaCompile>("compileLauncherJava").configure {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        options.compilerArgs.add("-Xlint:-options")
    }

    jar {
        inputs.property("archivesName", archivesBaseName)

        from("LICENSE") {
            rename { "${it}_$archivesBaseName" }
        }
        from("NOTICE") {
            rename { "${it}_$archivesBaseName" }
        }

        // Include launcher classes
        from(launcher.output)

        manifest {
            attributes["Main-Class"] = "com.gaspoweredcake.client33.Main"
        }
    }

    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked"
            )
        )

        options.errorprone.enabled.set(runErrorProne)

        if (runErrorProne) {
            options.errorprone {
                check("NullAway", net.ltgt.gradle.errorprone.CheckSeverity.ERROR)
                option("NullAway:AnnotatedPackages", "com.gaspoweredcake.client33")
                option("NullAway:JSpecifyMode", "true")
                // Event handlers are discovered reflectively by Orbit.
                option("UnusedMethod:ExcludedAnnotations", "meteordevelopment.orbit.EventHandler")
            }
        }
    }

    javadoc {
        with(options as StandardJavadocDocletOptions) {
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("encoding", "UTF-8")
            addStringOption("charSet", "UTF-8")
        }
    }

    build {
        if (System.getenv("CI")?.toBoolean() == true) {
            dependsOn("javadocJar")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "client33"

            version = "${libs.versions.minecraft.get()}-SNAPSHOT"
        }
    }

}

// Pure module math can be checked without launching a Minecraft client.
val regression = sourceSets.create("regression") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
}
val moduleLogicRegression = tasks.register<JavaExec>("moduleLogicRegression") {
    group = "verification"
    description = "Checks persistence failures, recon parsing, combat timing/risk, and ESP projection boundaries."
    dependsOn(tasks.named(regression.classesTaskName))
    classpath = regression.runtimeClasspath
    mainClass.set("com.gaspoweredcake.client33.ModuleLogicRegression")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    })
}
tasks.check { dependsOn(moduleLogicRegression) }

// A development classpath can hide libraries missing from a distributed Fabric JAR.
val verifyBundledRuntime = tasks.register("verifyBundledRuntime") {
    group = "verification"
    dependsOn(tasks.jar)
    val archive = tasks.jar.flatMap { it.archiveFile }
    inputs.file(archive)
    doLast {
        val required = mapOf(
            "javassist-3.28.0-GA.jar" to "javassist/bytecode/ClassFile.class",
            "types-0.2.0.jar" to "io/github/humbleui/types/RRect.class"
        )
        ZipFile(archive.get().asFile).use { zip ->
            val metadata = zip.getInputStream(zip.getEntry("fabric.mod.json")).bufferedReader().readText()
            required.forEach { (jar, clazz) ->
                val path = "META-INF/jars/$jar"
                check(metadata.contains(path)) { "$path is not declared in fabric.mod.json" }
                val entry = zip.getEntry(path) ?: error("Missing bundled runtime library $path")
                val found = ZipInputStream(zip.getInputStream(entry)).use { nested ->
                    var current = nested.nextEntry
                    while (current != null && current.name != clazz) current = nested.nextEntry
                    current != null
                }
                check(found) { "$path does not contain $clazz" }
            }
        }
    }
}
tasks.check { dependsOn(verifyBundledRuntime) }
