package org.gradle.client.hacking

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class RootProjectPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        tasks.register<DeclarativeReport>("declarativeReport") {
            group = "declarative"
            notCompatibleWithConfigurationCache("Uses the settings instance")
        }

        tasks.register<SetJavaVersionMutationTask>("setJavaVersion") {
            group = "declarative"
            notCompatibleWithConfigurationCache("Uses the settings instance")
        }
    }
}