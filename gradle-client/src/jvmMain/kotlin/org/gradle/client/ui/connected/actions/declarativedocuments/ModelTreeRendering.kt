package org.gradle.client.ui.connected.actions.declarativedocuments

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.times
import org.gradle.client.core.gradle.dcl.isUnresolvedBase
import org.gradle.client.core.gradle.dcl.type
import org.gradle.client.ui.composables.LabelMedium
import org.gradle.client.ui.composables.TitleSmall
import org.gradle.client.ui.composables.semiTransparentIfNull
import org.gradle.client.ui.connected.actions.*
import org.gradle.client.ui.connected.actions.declarativedocuments.HighlightingKind.EFFECTIVE
import org.gradle.client.ui.connected.actions.declarativedocuments.HighlightingKind.SHADOWED
import org.gradle.client.ui.theme.spacing
import org.gradle.client.ui.theme.transparency
import org.gradle.declarative.dsl.schema.DataClass
import org.gradle.declarative.dsl.schema.DataProperty
import org.gradle.declarative.dsl.schema.SchemaMemberFunction
import org.gradle.internal.declarativedsl.analysis.TypeRefContext
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.ElementNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.PropertyNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.PropertyNode.PropertyAugmentation.Plus
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.ValueNode.*
import org.gradle.internal.declarativedsl.dom.DocumentNodeContainer
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ElementResolution.SuccessfulElementResolution.ContainerElementResolved
import org.gradle.internal.declarativedsl.dom.DocumentResolution.PropertyResolution.PropertyAssignmentResolved
import org.gradle.internal.declarativedsl.dom.data.NodeData
import org.gradle.internal.declarativedsl.dom.mutation.ApplicableMutation
import org.gradle.internal.declarativedsl.dom.mutation.MutationArgumentContainer
import org.gradle.internal.declarativedsl.dom.mutation.MutationDefinition
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin.*
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayOriginContainer
import org.gradle.internal.declarativedsl.dom.resolution.DocumentResolutionContainer
import org.jetbrains.skiko.Cursor

internal class ModelTreeRendering(
    private val typeRefContext: TypeRefContext,
    private val resolutionContainer: DocumentResolutionContainer,
    private val overlayOriginContainer: OverlayOriginContainer,
    private val highlightingContext: HighlightingContext,
    private val mutationApplicability: NodeData<List<ApplicableMutation>>,
    private val onRunMutation: (MutationDefinition, MutationArgumentContainer) -> Unit
) {
    private val indentDp = MaterialTheme.spacing.level2

    @Composable
    fun ElementInfoOrNothingDeclared(
        type: DataClass?,
        node: DocumentNodeContainer?,
        indentLevel: Int,
    ) {
        Column(Modifier.padding(start = indentLevel * indentDp)) {
            val isUnresolvedBaseNode = node is ElementNode && resolutionContainer.isUnresolvedBase(node)

            if (node == null || type == null || isUnresolvedBaseNode) {
                LabelMedium(modifier = Modifier.alpha(MaterialTheme.transparency.HALF), text = NOT_DECLARED)
            } else {
                MaterialTheme.spacing.VerticalLevel2()
                type.properties.forEach { property ->
                    val propertyNodes = node.content.filterIsInstance<PropertyNode>()
                        .filter { it.name == property.name }
                    PropertyInfo(propertyNodes, property)
                }
                val accessAndConfigure = type.memberFunctions.accessAndConfigure
                val accessAndConfigureNames = accessAndConfigure.map { it.simpleName }
                val addAndConfigure = type.memberFunctions.addAndConfigure.filter { function ->
                    function.simpleName !in accessAndConfigureNames
                }
                val addAndConfigureByName = addAndConfigure.associateBy { it.simpleName }
                val elementsByAddAndConfigure = node.content
                    .filterIsInstance<ElementNode>()
                    .filter { it.name in addAndConfigureByName }

                accessAndConfigure.forEach { subFunction ->
                    ConfiguringFunctionInfo(subFunction, node, indentLevel)
                    MaterialTheme.spacing.VerticalLevel2()
                }
                elementsByAddAndConfigure.forEach { element ->
                    AddingFunctionInfo(element, indentLevel)
                }
            }
        }
    }

    @Composable
    private fun AddingFunctionInfo(
        element: ElementNode,
        indentLevel: Int
    ) {
        if (resolutionContainer.isUnresolvedBase(element)) return

        val arguments = elementArgumentsString(element)
        val elementType = (resolutionContainer.data(element) as? ContainerElementResolved)?.elementType as? DataClass

        val elementTextRepresentation = "${element.name}($arguments)"

        val isEmpty = elementType == null || element.content.isEmpty()
        WithDecoration(element) {
            if (isEmpty) {
                LabelMedium(
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.level2)
                        .withHoverCursor()
                        .withClickTextRangeSelection(element, highlightingContext)
                        .semiTransparentIfNull(element),
                    text = elementTextRepresentation
                )
            } else {
                TitleSmall(
                    text = elementTextRepresentation,
                    modifier = Modifier
                        .withHoverCursor()
                        .withClickTextRangeSelection(element, highlightingContext)
                        .semiTransparentIfNull(element)
                )
            }
        }
        if (!isEmpty) {
            ElementInfoOrNothingDeclared(
                elementType,
                element,
                indentLevel + 1
            )
        }
    }

    private fun elementArgumentsString(element: ElementNode) =
        element.elementValues.joinToString { valueNode ->
            when (valueNode) {
                is LiteralValueNode -> valueNode.value.toString()
                is NamedReferenceNode -> valueNode.referenceName
                is ValueFactoryNode -> {
                    val args = valueNode.values.map {
                        (it as? LiteralValueNode)?.value ?: "..."
                    }
                    val argsString = args.joinToString(",", "(", ")")
                    "${valueNode.factoryName}$argsString"
                }
            }
        }

    @Composable
    private fun ConfiguringFunctionInfo(
        subFunction: SchemaMemberFunction,
        parentNode: DocumentNodeContainer,
        indentLevel: Int
    ) {
        if (parentNode is ElementNode && resolutionContainer.isUnresolvedBase(parentNode))
            return

        val functionNodes = parentNode.childElementNodes(resolutionContainer, subFunction)
        if (functionNodes.isNotEmpty()) {
            functionNodes.forEach { functionNode ->
                val functionType = functionNode.type(resolutionContainer) as? DataClass

                WithDecoration(functionNode) {
                    val argsString =
                        if (functionNode.elementValues.isNotEmpty()) "(${elementArgumentsString(functionNode)})" else ""
                    val title = subFunction.simpleName + argsString
                    TitleSmall(
                        text = title,
                        modifier = Modifier
                            .withHoverCursor()
                            .semiTransparentIfNull(functionType)
                            .withClickTextRangeSelection(functionNode, highlightingContext)
                    )
                }
                ElementInfoOrNothingDeclared(functionType, functionNode, indentLevel + 1)
            }
        } else {
            TitleSmall(
                text = subFunction.simpleName,
                modifier = Modifier
                    .withHoverCursor()
                    .semiTransparentIfNull(null)
                    .withClickTextRangeSelection(null, highlightingContext)
            )
            ElementInfoOrNothingDeclared(null, null, indentLevel + 1)
        }
    }


    @Composable
    private fun PropertyInfo(
        propertyNodes: List<PropertyNode>,
        property: DataProperty
    ) {
        if (propertyNodes.isNotEmpty() && propertyNodes.all { resolutionContainer.isUnresolvedBase(it) }) {
            return
        }
        val representativeNode = propertyNodes.lastOrNull()
        val valuePresenter = representativeNode?.run {
            val propertyTypeRef = (resolutionContainer.data(this) as? PropertyAssignmentResolved)?.property?.valueType
            if (propertyTypeRef != null) {
                val propertyType = typeRefContext.resolveRef(propertyTypeRef)
                supportedJointAssignedValuePresentaters.find { it.supportsPropertyType(propertyType) }
            } else null
        }
        val valuesPresentation = valuePresenter?.jointAssignedValuesPresentation(propertyNodes, resolutionContainer)

        WithDecoration(representativeNode) {
            Column {
                val maybeInvalidDecoration = if (
                    representativeNode != null &&
                    resolutionContainer.data(representativeNode) !is PropertyAssignmentResolved
                ) TextDecoration.LineThrough
                else TextDecoration.None

                Column(Modifier.padding(start = indentDp)) {
                    LabelMedium(
                        modifier = Modifier.padding(bottom = MaterialTheme.spacing.level2)
                            .withHoverCursor()
                            .withClickTextRangeSelection(representativeNode, highlightingContext)
                            .semiTransparentIfNull(representativeNode),
                        textStyle = TextStyle(textDecoration = maybeInvalidDecoration),
                        text = buildString {
                            append("${property.name}: ${property.typeName}")
                            if (propertyNodes.size == 1) {
                                append(representativeNode?.augmentation?.let { if (it == Plus) " +=" else " =" } ?: ":")
                                append(
                                    propertyNodes.single().value.sourceData.text().let { " $it" }
                                )
                            }
                        }
                    )
                    if (propertyNodes.size == 1) {
                        valuesPresentation?.get(representativeNode)?.effectiveValueNodes.orEmpty().forEach {
                            PropertyValueItem(it)
                        }
                    } else {
                        if (propertyNodes.size > 1) {
                            propertyNodes.forEach { propertyNode ->
                                WithDecoration(propertyNode) {
                                    Column {
                                        LabelMedium(
                                            modifier = Modifier.padding(bottom = MaterialTheme.spacing.level2)
                                                .withHoverCursor()
                                                .withClickTextRangeSelection(propertyNode, highlightingContext, false)
                                                .semiTransparentIfNull(representativeNode),
                                            textStyle = TextStyle(textDecoration = maybeInvalidDecoration),
                                            text = "${if (propertyNode.augmentation == Plus) "+=" else "="} ${
                                                propertyNode.value.sourceData.text()
                                            }"
                                        )
                                        valuesPresentation?.get(propertyNode)?.effectiveValueNodes.orEmpty().forEach {
                                            PropertyValueItem(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PropertyValueItem(
        node: DeclarativeDocument.ValueNode
    ) {
        WithDecoration(node) {
            LabelMedium(
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.level2, start = indentDp)
                    .withHoverCursor()
                    .withClickTextRangeSelection(node, highlightingContext),
                text = "• ${node.sourceData.text()}"
            )
        }
    }

    @Composable
    fun WithDecoration(
        node: DeclarativeDocument.Node?,
        content: @Composable () -> Unit
    ) {
        WithApplicableMutations(node) {
            WithModelHighlighting(node) {
                content()
            }
        }
    }

    @Composable
    private fun WithModelHighlighting(
        node: DeclarativeDocument.Node?,
        content: @Composable () -> Unit,
    ) {
        val relevantSourceRanges: List<FileAndRange> = relevantSourceRanges(node)

        val highlightingKind = run {
            fun FileAndRange.matchesHighlightingEntry(highlightingEntry: HighlightingEntry): Boolean =
                fileId == highlightingEntry.fileIdentifier &&
                    range.first in highlightingEntry.range && range.last in highlightingEntry.range

            highlightingContext.highlightedRanges.firstOrNull { entry ->
                relevantSourceRanges.any { it.matchesHighlightingEntry(entry) }
            }?.highlightingKind
        }

        if (highlightingKind != null) {
            Surface(shape = MaterialTheme.shapes.extraSmall, color = highlightingKind.highlightingColor()) {
                content()
            }
        } else {
            content()
        }
    }

    data class FileAndRange(val fileId: String, val range: IntRange)

    fun DeclarativeDocument.Node.fileAndRange(): FileAndRange =
        sourceData.run { FileAndRange(sourceIdentifier.fileIdentifier, indexRange) }

    private fun relevantSourceRanges(node: DeclarativeDocument.Node?): List<FileAndRange> {
        val origin = when (node) {
            is DeclarativeDocument.DocumentNode -> overlayOriginContainer.data(node)
            is DeclarativeDocument.ValueNode -> overlayOriginContainer.data(node)
            else -> null
        }
        return when (origin) {
            is FromOverlay -> listOfNotNull(node?.fileAndRange())
            is FromUnderlay -> listOfNotNull(node?.fileAndRange())
            is MergedProperties -> origin.run {
                listOf(
                    shadowedPropertiesFromUnderlay,
                    shadowedPropertiesFromOverlay,
                    effectivePropertiesFromUnderlay,
                    effectivePropertiesFromOverlay
                ).flatten().map { it.fileAndRange() }
            }

            is MergedElements -> origin.run {
                listOf(underlayElement, overlayElement).map { it.fileAndRange() }
            }

            null -> listOf()
        }
    }

    @Composable
    private fun WithApplicableMutations(
        element: DeclarativeDocument.Node?,
        content: @Composable () -> Unit
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.level1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()

            if (element is DeclarativeDocument.DocumentNode) {
                ApplicableMutations(element, mutationApplicability, onRunMutation)
            }
        }
    }
}

internal fun Modifier.withHoverCursor() =
    pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.withClickTextRangeSelection(
    node: DeclarativeDocument.Node?,
    highlightingContext: HighlightingContext,
    highlightWithOverlay: Boolean = true
) = onClick {
    if (node == null) {
        highlightingContext.clearHighlighting()
    } else {
        highlightingContext.setHighlightingRanges(
            if (highlightWithOverlay) {
                highlightingForNodesOf(node, highlightingContext.overlayOriginContainer)
            } else {
                listOf(node.highlightAs(EFFECTIVE))
            }
        )
    }
}

internal fun DeclarativeDocument.Node.highlightAs(
    highlightingKind: HighlightingKind
) = HighlightingEntry(sourceData.sourceIdentifier.fileIdentifier, sourceData.indexRange, highlightingKind)

internal fun highlightingForNodesOf(
    node: DeclarativeDocument.Node,
    overlayOriginContainer: OverlayOriginContainer
): List<HighlightingEntry> {
    val origin = when (node) {
        is DeclarativeDocument.DocumentNode -> overlayOriginContainer.data(node)
        is DeclarativeDocument.ValueNode -> overlayOriginContainer.data(node)
    }
    return when (origin) {
        is FromOverlay,
        is FromUnderlay -> listOf(node.highlightAs(EFFECTIVE))

        is MergedElements -> listOf(
            node.highlightAs(EFFECTIVE),
            origin.underlayElement.highlightAs(EFFECTIVE)
        )

        is MergedProperties ->
            (origin.effectivePropertiesFromOverlay + origin.effectivePropertiesFromUnderlay).map {
                it.highlightAs(EFFECTIVE)
            } + (origin.shadowedPropertiesFromOverlay + origin.shadowedPropertiesFromUnderlay).map {
                it.highlightAs(SHADOWED)
            }
    }
}

internal data class HighlightingContext(
    private val viewModel: GetDeclarativeDocumentsModel,
    val overlayOriginContainer: OverlayOriginContainer,
) {
    val highlightedRanges: List<HighlightingEntry>
        get() = viewModel.highlightedSourceRangeByFileId.value

    fun clearHighlighting() = viewModel.clearHighlighting()
    fun setHighlightingRanges(entries: List<HighlightingEntry>) = viewModel.setHighlightingRanges(entries)
}

private const val NOT_DECLARED = "Not declared"

