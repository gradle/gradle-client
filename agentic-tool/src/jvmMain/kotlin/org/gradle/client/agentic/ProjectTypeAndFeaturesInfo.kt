package org.gradle.client.agentic

import org.gradle.declarative.dsl.schema.ProjectFeatureOrigin
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.DocumentResolution
import org.gradle.internal.declarativedsl.dom.operations.overlay.DocumentOverlayResult

internal data class ProjectTypeAndFeaturesInfo(
    val projectType: ProjectFeatureOrigin?,
    val projectFeatures: List<ProjectFeatureOrigin>,
) {
    val projectTypeName = projectType?.featureName
    val projectFeatureNames = projectFeatures.map { it.featureName }.toSet()
}

@Suppress("NestedBlockDepth")
internal fun collectProjectTypeAndFeatures(
    fullDom: DocumentOverlayResult,
): ProjectTypeAndFeaturesInfo {
    var projectType: ProjectFeatureOrigin? = null
    val projectFeatures = mutableSetOf<ProjectFeatureOrigin>()

    fun visitNode(node: DeclarativeDocument.DocumentNode) {
        when (node) {
            is DeclarativeDocument.DocumentNode.ElementNode -> {
                when (val resolution = fullDom.overlayResolutionContainer.data(node)) {
                    is DocumentResolution.ElementResolution.ElementNotResolved -> Unit
                    is DocumentResolution.ElementResolution.SuccessfulElementResolution -> {
                        val function = resolution.elementFactoryFunction
                        function.metadata.filterIsInstance<ProjectFeatureOrigin>().forEach { metadata ->
                            val isProjectType =
                                metadata.targetDefinitionClassName == "org.gradle.api.Project"

                            if (isProjectType) {
                                projectType = metadata
                            } else {
                                projectFeatures += metadata
                            }
                        }
                    }
                }
                node.content.forEach(::visitNode)
            }

            is DeclarativeDocument.DocumentNode.ErrorNode -> Unit
            is DeclarativeDocument.DocumentNode.PropertyNode -> Unit
        }
    }

    fullDom.document.content.forEach(::visitNode)

    return ProjectTypeAndFeaturesInfo(projectType, projectFeatures.toList())
}
