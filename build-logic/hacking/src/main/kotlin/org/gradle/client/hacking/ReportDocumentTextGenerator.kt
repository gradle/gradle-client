package org.gradle.client.hacking

import org.gradle.client.hacking.mutations.MutationApplicabilityContainer
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.mutation.common.NewDocumentNodes
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayOriginContainer


internal
class ReportCodeGenerator {
    fun valueNodeString(node: DeclarativeDocument.ValueNode): String = when (node) {
        is DeclarativeDocument.ValueNode.LiteralValueNode -> when (val value = node.value) {
            is String -> "\"$value\""
            else -> value.toString()
        }

        is DeclarativeDocument.ValueNode.ValueFactoryNode -> if (node.isInfix) {
            check(node.values.size == 2) { 
                "An infix call value factory node must have exactly two values, got: ${node.values}" 
            }
            "${valueNodeString(node.values[0])} ${node.factoryName} ${valueNodeString(node.values[1])}"
        } else {
            "${node.factoryName}(${node.values.joinToString { valueNodeString(it) }})"
        }

        is DeclarativeDocument.ValueNode.NamedReferenceNode -> node.referenceName
    }

    @Suppress("CyclomaticComplexMethod")
    fun generateCode(
        nodes: NewDocumentNodes,
        indentProvider: (Int) -> String,
        isTopLevel: Boolean,
        overlayOrigins: OverlayOriginContainer,
        mutationApplicability: MutationApplicabilityContainer 
    ) = buildString {
        fun visitNode(node: DeclarativeDocument.DocumentNode, depth: Int = 0) {
            fun indent() = indentProvider(depth)

            when (node) {
                is DeclarativeDocument.DocumentNode.PropertyNode -> {
                    val overlayOrigin = overlayOrigins.data(node)
                    
                    val defaultsSuffix = when (val origin = overlayOrigin) {
                        is OverlayNodeOrigin.FromOverlay -> ""
                        is OverlayNodeOrigin.FromUnderlay -> " (from defaults)"
                        is OverlayNodeOrigin.MergedProperties -> {
                            " (overrides " +
                                origin.shadowedPropertiesFromUnderlay.joinToString { valueNodeString(it.value) } +
                                ")"
                        }
                    }
                    
                    val applicability = mutationApplicability.data(node)
                    val mutationSuffix = when {
                        applicability.isEmpty() -> ""
                        else -> " (mutations: ${applicability.joinToString { it.mutationDefinition.id }})"
                    }
                    
                    val operator = when (node.augmentation) {
                        DeclarativeDocument.DocumentNode.PropertyNode.PropertyAugmentation.None -> "="
                        DeclarativeDocument.DocumentNode.PropertyNode.PropertyAugmentation.Plus -> "+="
                    }
                    append(
                        "${indent()}${node.name} $operator ${valueNodeString(node.value)}" +
                            defaultsSuffix +
                            mutationSuffix
                    )
                }

                is DeclarativeDocument.DocumentNode.ElementNode -> {
                    val representation = nodes.representationFlags.data(node)
                    append("${indent()}${node.name}")
                    if (node.elementValues.isNotEmpty() || node.content.isEmpty() && !representation.forceEmptyBlock) {
                        append("(${node.elementValues.joinToString { valueNodeString(it) }})")
                    }
                    if (node.content.isNotEmpty()) {
                        appendLine(" {")
                        node.content.forEach {
                            visitNode(it, depth + 1)
                            appendLine()
                        }
                        append("${indent()}}")
                    } else {
                        if (representation.forceEmptyBlock) {
                            append(" { }")
                        }
                    }
                }

                is DeclarativeDocument.DocumentNode.ErrorNode -> 
                    append("${indent()}${node.sourceData.text()}")
                
            }
        }

        nodes.nodes.forEachIndexed { index, item ->
            if (index > 0) {
                appendLine()
                if (isTopLevel && (nodes.nodes[index - 1] is DeclarativeDocument.DocumentNode.ElementNode 
                        || item is DeclarativeDocument.DocumentNode.ElementNode)) {
                    appendLine()
                }
            }
            visitNode(item)
        }
    }
}
