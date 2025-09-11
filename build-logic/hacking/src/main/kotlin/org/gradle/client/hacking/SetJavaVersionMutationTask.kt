package org.gradle.client.hacking

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option
import org.gradle.client.hacking.mutations.SetJavaVersion
import org.gradle.declarative.dsl.evaluation.EvaluationSchema
import org.gradle.internal.declarativedsl.dom.mutation.*
import org.gradle.internal.declarativedsl.dom.resolution.DocumentWithResolution
import org.gradle.internal.declarativedsl.evaluator.main.AnalysisDocumentUtils
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator
import org.gradle.internal.declarativedsl.evaluator.runner.stepResultOrPartialResult
import java.io.File

@UntrackedTask(because = "Only runs a mutation")
abstract class SetJavaVersionMutationTask : DefaultTask() {

    @get:Nested
    protected abstract val definitionPrerequisitesProvider: DefinitionPrerequisitesProvider

    private val prerequisites = run {
        definitionPrerequisitesProvider.get()
    }

    private val analyzer = SimpleAnalysisEvaluator.withSchema(
        prerequisites.settingsInterpretationSequence,
        prerequisites.projectInterpretationSequence
    )

    private val settingsFile = listOfNotNull(
        "settings.gradle.dcl",
        "settings.gradle.kts"
    ).mapNotNull { project.rootProject.file(it).takeIf { it.canRead() } }.single()

    @TaskAction
    fun generate() {
        val dclFile = dclFile.get().asFile

        val settingsResult = analyzer.evaluate(settingsFile.name, settingsFile.readText())
        val projectResult = analyzer.evaluate(dclFile.name, dclFile.readText())

        val dom = AnalysisDocumentUtils.documentWithModelDefaults(settingsResult, projectResult)
        val evaluationSchema = projectResult.stepResults.values.single().stepResultOrPartialResult.evaluationSchema

        runMutation(dclFile, dom!!.inputOverlay, evaluationSchema, SetJavaVersion, mutationArguments {
            argument(SetJavaVersion.javaVersionParameter, javaVersion.get())
        })
    }

    @get:Option("dclFile", "path to the DCL file to produce the definition")
    @get:Internal
    abstract val dclFile: RegularFileProperty

    @get:Option("javaVersion", "Java version to set")
    @get:Internal
    abstract val javaVersion: Property<Int>
}

private fun runMutation(
    outFile: File,
    documentWithResolution: DocumentWithResolution,
    schema: EvaluationSchema,
    mutationDefinition: MutationDefinition,
    mutationArgumentsContainer: MutationArgumentContainer
) {
    val target = TextMutationApplicationTarget(documentWithResolution, schema)
    val result = MutationAsTextRunner().runMutation(mutationDefinition, mutationArgumentsContainer, target)

    result.stepResults.filterIsInstance<ModelMutationStepResult.ModelMutationStepApplied>().lastOrNull()?.let {
        outFile.writeText(it.newDocumentText)
    }
}
