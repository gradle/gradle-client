pluginManagement {
    includeBuild("../../now-in-android/declarative-gradle/unified-prototype/unified-plugin")
}

plugins {
    id("org.gradle.experimental.plugin-ecosystem").version("0.1.40")
}

dependencyResolutionManagement {
    repositoriesMode = FAIL_ON_PROJECT_REPOS
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("plugins")

rootProject.name = "declarative"
