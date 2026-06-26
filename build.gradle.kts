plugins {
    kotlin("jvm") version "2.2.0"
    id("maven-publish")
}

allprojects {
    group = "ru.nnedition.ymdownloader"
    version = "2.1"

    repositories {
        maven("https://jitpack.io")
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    kotlin {
        jvmToolchain(21)
    }

    java {
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}