dependencyResolutionManagement {
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

// Check out this project as a sibling to now-in-android, that contains the declarative prototype project to use this
// includeBuild("../../now-in-android/declarative-gradle/unified-prototype/unified-plugin")

rootProject.name = "declarative"

include("plugins")
