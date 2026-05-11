pluginManagement {
    includeBuild("./build-logic")
}

plugins {
    id("com.gradle.develocity").version("4.3.2")
    id("io.github.gradle.develocity-conventions-plugin").version("0.14.1")
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.8.0")

    id("org.gradle.experimental.jvm-ecosystem").version("0.1.61")
    id("org.gradle.experimental.kmp-ecosystem").version("0.1.61")
    id("org.gradle.client.ecosystem.custom-ecosystem")
}

dependencyResolutionManagement {
    repositoriesMode = FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        gradlePluginPortal()
        maven {
            url = uri("https://repo.gradle.org/gradle/libs-releases")
        }
        maven {
            url = uri("https://repo.gradle.org/gradle/libs-snapshots")
        }
        mavenCentral()
    }
}

rootProject.name = "gradle-client-root"

include(":gradle-client")
include(":build-action")
include(":mutations-demo")
include(":agentic-tool")
include(":dcl-utils")

defaults {
    kotlinApplication {
        detekt {
            parallel = true
            source = listOf(layout.projectDirectory.dir("src/jvmMain/kotlin"), layout.projectDirectory.dir("src/jvmTest/kotlin"))
            config = listOf(layout.settingsDirectory.file("gradle/detekt/detekt.conf"))
        }
    }
    kotlinJvmApplication {
        javaVersion = 8
    }
    kotlinJvmLibrary {
        javaVersion = 8
    }
    javaLibrary {
        javaVersion = 8
    }
}
