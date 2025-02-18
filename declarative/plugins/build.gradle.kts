plugins {
    id("java-gradle-plugin")
}

description = "Declarative plugins containing custom software types for the gradle-client project."

dependencies {
    api(libs.declarative)
    api(libs.detekt)
    api(libs.jetbrains.compose)
    api(libs.kotlin.compose)
    api(libs.kotlin.multiplatform)
    api(libs.sqldelight)
}

gradlePlugin {
    plugins {
        create("custom-ecosystem") {
            id = "org.gradle.client.ecosystem.custom-ecosystem"
            implementationClass = "org.gradle.client.ecosystem.CustomEcosystemPlugin"
        }

        create("desktop-compose-application") {
            id = "org.gradle.client.softwaretype.desktop-compose-application"
            implementationClass = "org.gradle.client.softwaretype.CustomDesktopComposeApplicationPlugin"
        }
    }
}

// TODO: This is more properly done as part of the root level settings file, but isn't supported in DCL
// move this to daemon toolchain once Gradle supports it
require(JavaVersion.current() == JavaVersion.VERSION_17) {
    "This build requires Java 17, currently using ${JavaVersion.current()}"
}