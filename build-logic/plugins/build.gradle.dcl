javaGradlePlugin {
    description = "Declarative plugins containing custom software types for the gradle-client project."

    dependencies {
        api("org.gradle.experimental.kmp-ecosystem:org.gradle.experimental.kmp-ecosystem.gradle.plugin:0.1.46")
        api("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:2.2.0")
        api("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:2.2.0")
        api("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.2.0")
        api("org.jetbrains.compose:compose-gradle-plugin:1.6.11")
        api("app.cash.sqldelight:gradle-plugin:2.0.2")
        api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.6")
    }

    registers {
        id("org.gradle.client.ecosystem.custom-ecosystem") {
            description = "A custom ecosystem plugin registering the Software Type plugins provided by this project"
            implementationClass = "org.gradle.client.ecosystem.CustomEcosystemPlugin"
        }
    }
}
