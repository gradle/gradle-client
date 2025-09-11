package org.gradle.client.hacking

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class DeclarativeReport : DefaultTask() {
    @TaskAction
    fun generate() {
        println("Generating declarative report...")
    }
}