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
