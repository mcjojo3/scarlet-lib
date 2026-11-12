pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.glass-launcher.net/babric")
        mavenCentral()
        gradlePluginPortal()
    }
    // Versions live here, so build.gradle.kts can also be used as a subproject of a build that
    // already applies these plugins (CreRaces and Twilight Lib include this project that way).
    plugins {
        id("fabric-loom") version "1.17.20"
        id("babric-loom-extension") version "1.17.5"
    }
}

rootProject.name = "scarlet-lib"
