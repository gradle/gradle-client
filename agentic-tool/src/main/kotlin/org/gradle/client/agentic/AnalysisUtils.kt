package org.gradle.client.agentic

import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.internal.declarativedsl.dom.operations.overlay.DocumentOverlayResult
import org.gradle.internal.declarativedsl.evaluator.main.AnalysisDocumentUtils
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator

fun getDomForProject(
    analyzer: SimpleAnalysisEvaluator,
    prerequisites: ResolvedDomPrerequisites,
    projectInfo: ResolvedDomPrerequisites.ProjectInfo
): DocumentOverlayResult? {
    val settingsScript = prerequisites.settingsFile.takeIf { it.canRead() }?.readText() ?: ""
    val projectScript = projectInfo.projectBuildFile.takeIf { it.canRead() }?.readText() ?: ""

    val settingsResult =
        analyzer.evaluate(prerequisites.settingsFile.path, settingsScript)
    val projectResult =
        analyzer.evaluate(projectInfo.projectBuildFile.path, projectScript)

    return AnalysisDocumentUtils.documentWithModelDefaults(settingsResult, projectResult)
}
