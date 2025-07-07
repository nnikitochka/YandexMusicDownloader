plugins {
    kotlin("jvm") version "2.1.20"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "ru.nnedition.ymdownloader"
    version = "0.2.2"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    kotlin {
        jvmToolchain(21)
    }

}