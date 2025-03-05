pluginManagement {
    includeBuild("./declarative-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.enterprise").version("3.19.2")
    id("io.github.gradle.gradle-enterprise-conventions-plugin").version("0.10.2")
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.8.0")

    id("org.gradle.experimental.jvm-ecosystem").version("0.1.41")
    id("org.gradle.experimental.kmp-ecosystem").version("0.1.41")
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
