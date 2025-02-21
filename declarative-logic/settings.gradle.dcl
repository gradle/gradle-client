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

rootProject.name = "declarative"

include("plugins")
