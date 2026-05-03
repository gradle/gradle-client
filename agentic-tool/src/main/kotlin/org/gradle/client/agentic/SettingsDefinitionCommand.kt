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
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.ElementNode
import org.gradle.internal.declarativedsl.dom.DocumentResolution
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ElementResolution.SuccessfulElementResolution
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator
import settingsWithNoOverlayOrigin

class SettingsDefinitionCommand : DclCommand("settings-definition") {

    override fun help(context: Context): String =
        "Prints the effective settings definition (including defaults declared for project types). " +
            "By default, omits schema properties that have no values and nested objects that are not used."

    val withUnusedMembers by option(
        help = "Include members that are not present in the definition or defaults"
    ).boolean().default(false)

    override fun produceOutput(
        prerequisites: ResolvedDomPrerequisites,
        analyzer: SimpleAnalysisEvaluator
    ): JsonElement {
        val settingsScript = prerequisites.settingsFile.takeIf { it.canRead() }?.readText() ?: ""

        val settingsResult =
            analyzer.evaluate(prerequisites.settingsFile.path, settingsScript)
        val dom = settingsWithNoOverlayOrigin(settingsResult)
            ?: return buildJsonObject { put("error", "Model evaluation failed") }

        return buildJsonObject {
            put("settingsFile", prerequisites.settingsFile.path)
            put("content", buildJsonArray {
                with(DomRenderer(withUnusedMembers, dom)) {
                    dom.document.content.forEach { visitNode(it) }
                }
            })
        }
    }
}
