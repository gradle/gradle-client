plugins {
    `kotlin-dsl`
}

javaGradlePlugin {
    description = "hacking"

    dependencies {

    }

    registers {
        id("org.gradle.client.hacking.root") {
            description = "asdf"
            implementationClass = "org.gradle.client.hacking.RootProjectPlugin"
        }
    }
}
