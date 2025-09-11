package org.gradle.client.hacking.mutations

import org.gradle.declarative.dsl.schema.AnalysisSchema
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.data.NodeData
import org.gradle.internal.declarativedsl.dom.data.NodeDataContainer
import org.gradle.internal.declarativedsl.dom.mutation.ApplicableMutation
import org.gradle.internal.declarativedsl.dom.mutation.DefaultMutationDefinitionCatalog
import org.gradle.internal.declarativedsl.dom.mutation.applicabilityFor
import org.gradle.internal.declarativedsl.dom.operations.overlay.DocumentOverlayResult
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin.FromOverlay
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin.FromUnderlay
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin.MergedElements
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin.MergedProperties
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayOriginContainer

typealias MutationApplicabilityContainer = NodeData<List<ApplicableMutation>>

fun checkMutationApplicabilityForOverlay(
    modelSchema: AnalysisSchema,
    overlayResult: DocumentOverlayResult,
): MutationApplicabilityContainer {
    val overlayDocument = overlayResult.inputOverlay
    val compatibleMutationsCatalog = DefaultMutationDefinitionCatalog().apply {
        mutationCatalog.mutationDefinitionsById.values.forEach {
            if (it.isCompatibleWithSchema(modelSchema)) {
                registerMutationDefinition(it)
            }
        }
    }
    val applicability = compatibleMutationsCatalog.applicabilityFor(modelSchema, overlayDocument)

    return OverlayRoutedNodeDataContainer(
        overlayResult.overlayNodeOriginContainer,
        emptyListNodeDataContainer(),
        applicability
    )
}

internal class OverlayRoutedNodeDataContainer<DNode, DElement : DNode, DProperty : DNode, DError : DNode>(
    private val overlayOriginContainer: OverlayOriginContainer,
    private val underlay: NodeDataContainer<DNode, DElement, DProperty, DError>,
    private val overlay: NodeDataContainer<DNode, DElement, DProperty, DError>
) : NodeDataContainer<DNode, DElement, DProperty, DError> {
    override fun data(node: DeclarativeDocument.DocumentNode.ElementNode): DElement =
        when (val from = overlayOriginContainer.data(node)) {
            is FromUnderlay -> underlay.data(node)
            is FromOverlay -> overlay.data(node)
            is MergedElements -> overlay.data(from.overlayElement)
        }

    override fun data(node: DeclarativeDocument.DocumentNode.PropertyNode): DProperty =
        when (val from = overlayOriginContainer.data(node)) {
            is FromUnderlay -> underlay.data(node)
            is FromOverlay -> overlay.data(node)
            is MergedProperties -> when (node) {
                in from.effectivePropertiesFromUnderlay -> underlay.data(node)
                in from.effectivePropertiesFromOverlay -> overlay.data(node)
                else -> error("expected $node to be in the effective property nodes of the overlay origin $from")
            }
        }

    override fun data(node: DeclarativeDocument.DocumentNode.ErrorNode): DError =
        when (overlayOriginContainer.data(node)) {
            is FromUnderlay -> underlay.data(node)
            is FromOverlay -> overlay.data(node)
        }
}

private fun <T> emptyListNodeDataContainer(): NodeData<List<T>> = object : NodeData<List<T>> {
    override fun data(node: DeclarativeDocument.DocumentNode.ElementNode): List<T> = emptyList()
    override fun data(node: DeclarativeDocument.DocumentNode.ErrorNode): List<T> = emptyList()
    override fun data(node: DeclarativeDocument.DocumentNode.PropertyNode): List<T> = emptyList()
}

