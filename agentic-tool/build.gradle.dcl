kotlinJvmApplication {
    mainClass = "org.gradle.client.agentic.AgenticToolKt"
    jvmArguments += listOf("--enable-native-access=ALL-UNNAMED")

    dependencies {
        implementation(project(":build-action"))
        implementation(project(":dcl-utils"))

        implementation("org.gradle:gradle-tooling-api:9.6.0-20260511195019+0000")
        implementation("org.gradle:gradle-declarative-dsl-core:9.6.0-20260511195019+0000")
        implementation("org.gradle:gradle-declarative-dsl-evaluator:9.6.0-20260511195019+0000")
        implementation("org.gradle:gradle-declarative-dsl-tooling-models:9.6.0-20260511195019+0000")

        implementation("com.github.ajalt.clikt:clikt:5.0.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

        runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
    }
}
