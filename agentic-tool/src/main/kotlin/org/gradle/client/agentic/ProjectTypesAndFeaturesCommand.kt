package org.gradle.client.agentic

import com.github.ajalt.clikt.core.Context
import kotlinx.serialization.json.*
import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.declarative.dsl.schema.DataClass
import org.gradle.declarative.dsl.schema.ProjectFeatureOrigin
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator

class ProjectTypesAndFeaturesCommand : DclCommand("project-types-and-features") {

    override fun help(context: Context): String =
        "Lists project types and features available in this build. For each project type and feature," +
            "provides the ID of the ecosystem plugin that contributes the project type or feature. " +
            "For project features, specifies which definition or build model type the feature can be applied to."

    override fun produceOutput(
        prerequisites: ResolvedDomPrerequisites,
        analyzer: SimpleAnalysisEvaluator
    ): JsonElement {
        return buildJsonObject {
            val schema = prerequisites.projectInterpretationSequence.steps.last().evaluationSchemaForStep.analysisSchema

            val projectTypes = schema.topLevelReceiverType.memberFunctions.flatMap { fn ->
                fn.metadata.filterIsInstance<ProjectFeatureOrigin>().map { fn to it }
            }

            putJsonArray("projectTypes") {
                projectTypes.forEach { (function, projectType) ->
                    addJsonObject {
                        put("name", projectType.featureName)
                        put("ecosystemPluginId", projectType.ecosystemPluginId)

                        documentationProvider.functionDocumentation(function)?.let {
                            put("documentation", it)
                        }
                    }
                }
            }

            val features = schema.dataClassTypesByFqName.values.filterIsInstance<DataClass>()
                .flatMap {
                    it.memberFunctions.flatMap { fn ->
                        fn.metadata.filterIsInstance<ProjectFeatureOrigin>()
                            .filter { it.targetDefinitionClassName != "org.gradle.api.Project" }
                            .map { fn to it }
                    }
                }.distinctBy { (_, it) ->
                    listOf(
                        it.featureName,
                        it.ecosystemPluginId,
                        it.targetDefinitionClassName,
                        it.targetBuildModelClassName
                    )
                }

            putJsonArray("projectFeatures") {
                features.forEach { (function, feature) ->
                    addJsonObject {
                        put("name", feature.featureName)
                        put("ecosystemPluginId", feature.ecosystemPluginId)

                        if (feature.targetDefinitionClassName != null) {
                            put("targetDefinitionTypeName", feature.targetDefinitionClassName)
                        }
                        if (feature.targetBuildModelClassName != null) {
                            put("targetBuildModelTypeName", feature.targetBuildModelClassName)
                        }

                        documentationProvider.functionDocumentation(function)
                            ?.let { put("documentation", it) }
                    }
                }
            }
        }
    }
}
