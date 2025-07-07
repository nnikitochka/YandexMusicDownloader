plugins {
    id("com.gradleup.shadow") version "8.3.0"
}

group = "ru.nnedition.ymdownloader"
version = "1.0"

dependencies {
    implementation(project(":ymd-api"))

    implementation("com.moandjiezana.toml:toml4j:0.7.2")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("com.github.nnikitochka:YetAnotherLogger:1.1")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "ru.nnedition.ymdownloader.Main"
            attributes["Implementation-Title"] = "YandexMusicDownloader"
            attributes["Implementation-Version"] = version
        }
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("")
        archiveFileName.set("YandexMusicDownloader.jar")
    }
}