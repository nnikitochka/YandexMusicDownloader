plugins {
    id("com.gradleup.shadow") version "8.3.0"
    `maven-publish`
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

        enabled = false
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

// Конфигурация публикации
publishing {
    publications {
        create<MavenPublication>("ymd-console-client") {
            artifact(tasks["shadowJar"])

            pom {
                name.set("Yandex Music Downloader")
                url.set("https://github.com/nnikitochka/YandexMusicDownloader")

                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")
                    configurations.implementation.get().allDependencies.forEach {
                        if (it.group != null && it.name != "unspecified") {
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", it.group)
                            dependencyNode.appendNode("artifactId", it.name)
                            dependencyNode.appendNode("version", it.version)
                        }
                    }
                }
            }
        }
    }
}