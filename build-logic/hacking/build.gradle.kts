plugins {
    `kotlin-dsl`
}

javaGradlePlugin {
    description = "hacking"

    dependencies {
        implementation("org.gradle:gradle-declarative-dsl-core:9.0-milestone-1")
        implementation("org.gradle:gradle-declarative-dsl-evaluator:9.0-milestone-1")
        implementation("org.gradle:gradle-declarative-dsl-tooling-models:9.0-milestone-1")
    }

    registers {
        id("org.gradle.client.hacking.root") {
            description = "asdf"
            implementationClass = "org.gradle.client.hacking.RootProjectPlugin"
        }
    }
}
