pluginManagement {
    includeBuild("../../declarative-gradle/unified-prototype/unified-plugin")
}

plugins {
    id("org.gradle.experimental.plugin-ecosystem").version("0.1.41")
}

dependencyResolutionManagement {
    //repositoriesMode = FAIL_ON_PROJECT_REPOS
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("plugins")

rootProject.name = "build-logic"
