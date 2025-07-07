plugins {
    kotlin("jvm") version "2.1.20"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    kotlin {
        jvmToolchain(21)
    }

}