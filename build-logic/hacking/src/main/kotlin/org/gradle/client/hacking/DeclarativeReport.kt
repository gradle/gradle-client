package org.gradle.client.hacking

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.GradleInternal
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option
import org.gradle.client.hacking.mutations.SetJavaVersion
import org.gradle.client.hacking.mutations.checkMutationApplicabilityForOverlay
import org.gradle.declarative.dsl.evaluation.EvaluationSchema
import org.gradle.declarative.dsl.evaluation.InterpretationSequence
import org.gradle.internal.declarativedsl.dom.mutation.ModelMutationStepResult
import org.gradle.internal.declarativedsl.dom.mutation.MutationArgumentContainer
import org.gradle.internal.declarativedsl.dom.mutation.MutationAsTextRunner
import org.gradle.internal.declarativedsl.dom.mutation.MutationDefinition
import org.gradle.internal.declarativedsl.dom.mutation.TextMutationApplicationTarget
import org.gradle.internal.declarativedsl.dom.mutation.common.NewDocumentNodes
import org.gradle.internal.declarativedsl.dom.mutation.mutationArguments
import org.gradle.internal.declarativedsl.dom.resolution.DocumentWithResolution
import org.gradle.internal.declarativedsl.evaluationSchema.DefaultInterpretationSequence
import org.gradle.internal.declarativedsl.evaluationSchema.SimpleInterpretationSequenceStep
import org.gradle.internal.declarativedsl.evaluator.main.AnalysisDocumentUtils
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator
import org.gradle.internal.declarativedsl.evaluator.runner.stepResultOrPartialResult
import org.gradle.internal.declarativedsl.evaluator.schema.DeclarativeScriptContext
import org.gradle.internal.declarativedsl.evaluator.schema.DefaultEvaluationSchema
import org.gradle.internal.declarativedsl.evaluator.schema.InterpretationSchemaBuildingResult
import org.gradle.internal.declarativedsl.interpreter.GradleProcessInterpretationSchemaBuilder
import org.gradle.plugin.software.internal.SoftwareFeatureRegistry
import javax.inject.Inject

@UntrackedTask(because = "Only produces a report")
abstract class DeclarativeReport : DefaultTask() {

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

    private val reportCodeGenerator = ReportCodeGenerator()
    
    @TaskAction
    fun generate() {
        val dclFile = dclFile.get().asFile

        val settingsResult = analyzer.evaluate(settingsFile.name, settingsFile.readText())
        val projectResult = analyzer.evaluate(dclFile.name, dclFile.readText())

        val dom = AnalysisDocumentUtils.documentWithModelDefaults(settingsResult, projectResult)
        val overlayOrigins = dom!!.overlayNodeOriginContainer

        val analysisSchema =
            projectResult.stepResults.values.single().stepResultOrPartialResult.evaluationSchema.analysisSchema

        val applicability =
            checkMutationApplicabilityForOverlay(analysisSchema, dom)

        println(
            reportCodeGenerator.generateCode(
                NewDocumentNodes(dom.document.content),
                "  "::repeat,
                true,
                overlayOrigins,
                applicability
            )
        )
        // run the DCL interpreter
    }

    @get:Option("dclFile", "path to the DCL file to produce the definition")
    @get:Internal
    abstract val dclFile: RegularFileProperty
}


data class DefinitionPrerequisites(
    val projectInterpretationSequence: InterpretationSequence,
    val settingsInterpretationSequence: InterpretationSequence
)

abstract class DefinitionPrerequisitesProvider {
    @get:Inject
    abstract val softwareFeatureRegistry: SoftwareFeatureRegistry

    @get:Inject
    abstract val gradleInternal: GradleInternal

    fun get(): DefinitionPrerequisites {
        val schemaBuilder =
            GradleProcessInterpretationSchemaBuilder({ gradleInternal.settings }, softwareFeatureRegistry)

        val settingsSequence = schemaBuilder.getEvaluationSchemaForScript(DeclarativeScriptContext.SettingsScript)
            .sequenceOrError().analysisOnly()

        val projectSequence = schemaBuilder.getEvaluationSchemaForScript(DeclarativeScriptContext.ProjectScript)
            .sequenceOrError().analysisOnly()

        return DefinitionPrerequisites(projectSequence, settingsSequence)
    }

    private
    fun InterpretationSchemaBuildingResult.sequenceOrError() = when (this) {
        is InterpretationSchemaBuildingResult.InterpretationSequenceAvailable -> sequence
        // This is rather an unexpected case, should not happen in normal operation.
        InterpretationSchemaBuildingResult.SchemaNotBuilt -> throw GradleException("schema not available")
    }

    private
    fun InterpretationSequence.analysisOnly(): InterpretationSequence = DefaultInterpretationSequence(
        steps.map { step ->
            val evaluationSchema = step.evaluationSchemaForStep
            val analysisOnlySchema = DefaultEvaluationSchema(
                evaluationSchema.analysisSchema,
                evaluationSchema.analysisStatementFilter
            )
            SimpleInterpretationSequenceStep(step.stepIdentifier, step.features) { analysisOnlySchema }
        }
    )
}