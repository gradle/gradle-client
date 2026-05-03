package org.gradle.client.agentic

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.gradle.client.build.action.GetResolvedDomAction
import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator
import org.gradle.tooling.BuildAction
import org.gradle.tooling.GradleConnector
import java.io.File

fun main(args: Array<String>) {
    MainCommand()
        .subcommands(
            ProjectsCommand(),
            EffectiveProjectDefinitionCommand(),
            SettingsDefaultsCommand(),
            SettingsDefinitionCommand(),
            ProjectSchemaCommand()
        )
        .main(args)
}

class MainCommand : NoOpCliktCommand() {
    override fun help(context: Context): String =
        "An agent-friendly introspection tool for Declarative Gradle projects." +
            "\nEach command outputs a JSON on the standard output and has its own '<command> --help'."
}

abstract class DclCommand(name: String) : CliktCommand(name) {
    protected val rootProjectDir: String by option(help = "Gradle build root project directory").default(".")
    protected val documentationProvider: DocumentationProvider = NoopDocumentationProvider

    private val domPrerequisites: BuildAction<ResolvedDomPrerequisites> = GetResolvedDomAction()

    final override fun run() {
        val connector = GradleConnector.newConnector().forProjectDirectory(File(rootProjectDir).absoluteFile)
        val prerequisites = try {
            connector.connect().use { connection ->
                connection.action(domPrerequisites).addArguments("--no-scan", "--quiet").run()
            }
        } finally {
            connector.disconnect()
        }

        val analyzer = SimpleAnalysisEvaluator.withSchema(
            prerequisites.settingsInterpretationSequence,
            prerequisites.projectInterpretationSequence
        )

        val output = produceOutput(prerequisites, analyzer)
        println(Json { prettyPrint = true }.encodeToString(output))
    }

    abstract fun produceOutput(prerequisites: ResolvedDomPrerequisites, analyzer: SimpleAnalysisEvaluator): JsonElement
}


