kotlinJvmApplication {
    mainClass = "org.gradle.client.agentic.AgenticToolKt"

    dependencies {
        implementation(project(":build-action"))
        implementation(project(":dcl-utils"))

        implementation("org.gradle:gradle-tooling-api:9.6.0-milestone-1")
        implementation("org.gradle:gradle-declarative-dsl-core:9.6.0-milestone-1")
        implementation("org.gradle:gradle-declarative-dsl-evaluator:9.6.0-milestone-1")
        implementation("org.gradle:gradle-declarative-dsl-tooling-models:9.6.0-milestone-1")

        implementation("com.github.ajalt.clikt:clikt:5.0.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    }
}
