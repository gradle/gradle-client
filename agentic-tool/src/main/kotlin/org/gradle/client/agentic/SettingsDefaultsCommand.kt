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
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.ElementNode
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ElementResolution.SuccessfulElementResolution
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator
import settingsWithNoOverlayOrigin

class SettingsDefaultsCommand : DclCommand("settings-defaults") {

    override fun help(context: Context): String =
        "Lists settings defaults for project types. " +
            "If a project type list is specified, prints the defaults only for them. " +
            "By default, omits schema properties that have no values and nested objects that are not used."

    val projectTypes by option(
        help = "Comma-separated project type names for which to query settings defaults; " +
            "e.g. javaLibrary,javaApplication." +
            "If not specified, returns defaults for all project types"
    )

    val withUnusedMembers by option(
        help = "Include members that are not present in the defaults"
    ).boolean().default(false)

    override fun produceOutput(
        prerequisites: ResolvedDomPrerequisites,
        analyzer: SimpleAnalysisEvaluator
    ): JsonElement {
        val settingsScript = prerequisites.settingsFile.takeIf { it.canRead() }?.readText() ?: ""

        val projectTypeNames = commaSeparatedFeatureNames(projectTypes)

        val settingsResult =
            analyzer.evaluate(prerequisites.settingsFile.path, settingsScript)
        val dom = settingsWithNoOverlayOrigin(settingsResult)
            ?: return buildJsonObject { put("error", "Model evaluation failed") }

        return buildJsonObject {
            put("settingsFile", prerequisites.settingsFile.path)
            put("content", buildJsonArray {
                with(DomJsonRenderer(withUnusedMembers, dom)) {
                    val defaultsBlocks =
                        dom.document.content.filterIsInstance<ElementNode>()
                            .filter {
                                it.name == "defaults" &&
                                    dom.overlayResolutionContainer.data(it) is SuccessfulElementResolution
                            }

                    defaultsBlocks.forEach { defaultsBlock ->
                        defaultsBlock.content.forEach { node ->
                            if (node is ElementNode && (projectTypeNames == null || node.name in projectTypeNames)) {
                                visitNode(node)
                            }
                        }
                    }
                }
            })
        }
    }
}

internal fun commaSeparatedFeatureNames(projectTypes: String?): Set<String>? = projectTypes
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotBlank() }
    ?.toSet()
