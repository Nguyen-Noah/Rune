
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.22"
    }
}

rootProject.name = "Rune3D"
include("Rune")
include("Runestone")
