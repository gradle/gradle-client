pluginManagement {
    includeBuild("./declarative-logic")

    // Check out this project as a sibling to now-in-android, that contains the declarative prototype project to use this
    // includeBuild("../now-in-android/declarative-gradle/unified-prototype/unified-plugin")

    repositories {
        gradlePluginPortal()
        /*
            // TOOD: can't filter content searched in repo in DCL yet
            content {
                includeGroupAndSubgroups("com.gradle")
                includeGroupAndSubgroups("org.gradle")
                includeGroupAndSubgroups("io.github.gradle")
            }
        */
        mavenCentral()
    }
}

plugins {
    id("com.gradle.enterprise").version("3.16.2")
    id("io.github.gradle.gradle-enterprise-conventions-plugin").version("0.9.1")
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.8.0")

    id("org.gradle.experimental.jvm-ecosystem").version("0.1.40")
    id("org.gradle.experimental.kmp-ecosystem").version("0.1.40")
    id("org.gradle.client.ecosystem.custom-ecosystem")
}

dependencyResolutionManagement {
    // TODO:DCL Not supported in DCL yet
    // repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        /*
            // TOOD: can't filter content searched in repo in DCL yet
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        */
        gradlePluginPortal()
        /*
            // TOOD: can't filter content searched in repo in DCL yet
            content {
                includeGroupAndSubgroups("org.gradle")
            }
        */
        maven {
            url = uri("https://repo.gradle.org/gradle/libs-releases")
        }
        /*
            // TOOD: can't filter content searched in repo in DCL yet
            content {
                includeGroup("org.gradle")
            }
        */
        maven {
            url = uri("https://repo.gradle.org/gradle/libs-snapshots")
        }
        /*
            // TOOD: can't filter content searched in repo in DCL yet
            content {
                includeGroup("org.gradle")
            }
        */
        mavenCentral()
    }
}

rootProject.name = "gradle-client-root"

include(":gradle-client")
include(":build-action")
include(":mutations-demo")
