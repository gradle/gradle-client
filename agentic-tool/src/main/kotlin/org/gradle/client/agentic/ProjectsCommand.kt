package org.gradle.client.agentic

import com.github.ajalt.clikt.core.Context
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator

class ProjectsCommand : DclCommand("projects") {

    override fun help(context: Context): String =
        "Lists projects in the Gradle build. For each project, provides the " +
            "project path, project directory, project build file path. " +
            "If a project is Declarative, detects its project type and all project features used in it."

    override fun produceOutput(
        prerequisites: ResolvedDomPrerequisites,
        analyzer: SimpleAnalysisEvaluator
    ): JsonElement {
        return buildJsonObject {
            put("settingsFile", prerequisites.settingsFile.path)

            putJsonArray("projects") {
                prerequisites.projectInfos.forEach { projectInfo ->
                    addJsonObject project@{
                        put("gradlePath", projectInfo.projectPath)
                        put("projectDir", projectInfo.projectDirectory.path)
                        put("buildFile", projectInfo.projectBuildFile.path)

                        if (!projectInfo.isDeclarative) {
                            put("buildFileIsDeclarative", false)
                            return@project
                        }

                        val fullDom = getDomForProject(analyzer, prerequisites, projectInfo)
                            ?: return@project

                        val projectTypeAndFeatures = collectProjectTypeAndFeatures(fullDom)
                        put("projectType", projectTypeAndFeatures.projectTypeName)
                        putJsonArray("features") {
                            projectTypeAndFeatures.projectFeatureNames.forEach { feature ->
                                add(feature)
                            }
                        }
                    }
                }
            }
        }
    }
}
