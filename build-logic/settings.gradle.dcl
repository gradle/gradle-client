plugins {
    id("org.gradle.experimental.plugin-ecosystem").version("0.1.45")
}

dependencyResolutionManagement {
    repositoriesMode = FAIL_ON_PROJECT_REPOS
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("plugins")
include("hacking")

rootProject.name = "build-logic"
