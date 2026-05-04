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

            val projectTypes = schema.topLevelReceiverType.memberFunctions.flatMap {
                it.metadata.filterIsInstance<ProjectFeatureOrigin>()
            }

            putJsonArray("projectTypes") {
                projectTypes.forEach { projectType ->
                    addJsonObject {
                        put("name", projectType.featureName)
                        put("ecosystemPluginId", projectType.ecosystemPluginId)

                        val documentation = documentationProvider.featureDocumentation(
                            projectType.ecosystemPluginId,
                            projectType.featureName
                        )
                        if (documentation != null) {
                            put("documentation", documentation)
                        }
                    }
                }
            }

            val features = schema.dataClassTypesByFqName.values.filterIsInstance<DataClass>()
                .flatMap {
                    it.metadata.filterIsInstance<ProjectFeatureOrigin>()
                        .filter { it.targetDefinitionClassName != "org.gradle.api.Project" }
                }.distinctBy {
                    listOf(
                        it.featureName,
                        it.ecosystemPluginId,
                        it.targetDefinitionClassName,
                        it.targetBuildModelClassName
                    )
                }

            putJsonArray("projectFeatures") {
                features.forEach {
                    addJsonObject {
                        put("name", it.featureName)
                        put("ecosystemPluginId", it.ecosystemPluginId)

                        if (it.targetDefinitionClassName != null) {
                            put("targetDefinitionTypeName", it.targetDefinitionClassName)
                        }
                        if (it.targetBuildModelClassName != null) {
                            put("targetBuildModelTypeName", it.targetBuildModelClassName)
                        }

                        val documentation = documentationProvider.featureDocumentation(
                            it.ecosystemPluginId,
                            it.featureName
                        )
                        if (documentation != null) {
                            put("documentation", documentation)
                        }
                    }
                }
            }
        }
    }
}
