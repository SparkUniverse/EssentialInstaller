plugins {
    kotlin("jvm") version "2.3.0"
    application
    id("com.gradleup.shadow") version "8.3.0"
    kotlin("plugin.serialization") version "2.3.0"
}

group = "gg.essential"
version = "3.1.1"

repositories {
    mavenCentral()
    maven("https://repo.essential.gg/repository/maven-public")
    maven("https://maven.pkg.github.com/caoimhebyrne/JNApple")
}

dependencies {
    // See https://github.com/EssentialGG/Elementa for latest Elementa and UC build numbers
    val universalCraftVersion = 466
    implementation("gg.essential:universalcraft-standalone:$universalCraftVersion")

    val elementaVersion = 731
    implementation("gg.essential:elementa:$elementaVersion")
    implementation("gg.essential:elementa-unstable-statev2:$elementaVersion")
    implementation("gg.essential:elementa-unstable-layoutdsl:$elementaVersion")

    val log4jVersion = "2.24.2"
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")

    val ktorVersion = "3.0.1"
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    val jnaVersion = "5.14.0"
    implementation("net.java.dev.jna:jna:$jnaVersion")
    implementation("net.java.dev.jna:jna-platform:$jnaVersion")

    implementation("com.github.caoimhebyrne:JNApple:75ba28e")
}

kotlin.jvmToolchain(8)

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        moduleName = "installer"
    }
}

application {
    mainClass.set("gg.essential.installer.MainKt")
}

tasks.processResources {
    filesMatching("**/*.json") {
        expand("version" to project.version)
    }
}

// TODO maybe generate separate jars for different platforms? bit of a waste to ship linux and macos natives in the windows installer
tasks.shadowJar {
    archiveFileName = "installer.jar"
    // Otherwise, logging doesn't work. Found on https://stackoverflow.com/a/61475766
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
    minimize {
        exclude(dependency("org.lwjgl:lwjgl-nanovg:.*")) // segfaults in nvgCreate
        // TODO use proguard instead, cause this does retain quite a bunch of stuff we don't actually need
        exclude(dependency("gg.essential:universalcraft-standalone:.*")) // has classes used via ServiceLoader
        exclude(dependency("net.java.dev.jna:jna-platform:.*"))
    }
    mergeServiceFiles()
}

// These blow up because `:gui:elementa` and `:gui:standalone:elementa` both produce `elementa.jar`.
// And we don't need them anyway because we build a single monolithic jar instead.
tasks.distZip { enabled = false }
tasks.distTar { enabled = false }
