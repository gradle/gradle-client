package org.gradle.client.agentic

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator
import java.io.File

class EffectiveProjectDefinitionCommand : DclCommand("effective-project-definition") {
    override fun help(context: Context): String =
        "Prints the effective definition of a specified subproject. The effective definition is produced by applying " +
            "the project's definition as written in the build file on top of the defaults for the project type. " +
            "If a subproject path is not specified, prints the effective definition of the root project. " +
            "By default, omits schema properties that have no values and nested objects that are not used."

    val subprojectPath: String by option(
        help = "Gradle project path such as :foo:bar or filesystem path such as foo/bar"
    ).default(":")

    val withUnusedMembers by option(
        help = "Include members that are not present in the effective definition"
    ).boolean().default(false)

    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    override fun produceOutput(
        prerequisites: ResolvedDomPrerequisites,
        analyzer: SimpleAnalysisEvaluator
    ): JsonElement {
        val projectInfo = matchingProjectInfo(prerequisites, subprojectPath)
            ?: return buildJsonObject { put("error", "Project not found") }

        val fullDom = getDomForProject(analyzer, prerequisites, projectInfo)
            ?: return buildJsonObject { put("error", "Could not analyze the definition") }

        return buildJsonObject {
            put("gradlePath", projectInfo.projectPath)
            put("settingsFile", prerequisites.settingsFile.path)
            put("buildFile", projectInfo.projectBuildFile.path)
            put("content", buildJsonArray {
                with(DomJsonRenderer(withUnusedMembers, fullDom, documentationProvider)) {
                    fullDom.document.content.forEach { visitNode(it) }
                }
            })
        }
    }
}

internal fun matchingProjectInfo(
    prerequisites: ResolvedDomPrerequisites,
    projectPath: String
): ResolvedDomPrerequisites.ProjectInfo? =
    prerequisites.projectInfos.find {
        it.projectPath == projectPath
    } ?: prerequisites.projectInfos.find {
        it.projectDirectory.relativeToOrNull(File(".").absoluteFile)?.path?.equals(projectPath) == true
    } ?: prerequisites.projectInfos.find { it.projectDirectory.absolutePath == projectPath }
