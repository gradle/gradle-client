package org.gradle.client.ui.connected.actions

import org.gradle.declarative.dsl.schema.SchemaFunction
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.ElementNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.PropertyNode
import org.gradle.internal.declarativedsl.dom.DocumentNodeContainer
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ElementResolution
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ElementResolution.SuccessfulElementResolution
import org.gradle.internal.declarativedsl.dom.DocumentResolution.PropertyResolution
import org.gradle.internal.declarativedsl.dom.DocumentResolution.PropertyResolution.PropertyAssignmentResolved
import org.gradle.internal.declarativedsl.dom.resolution.DocumentResolutionContainer

val DeclarativeDocument.singleSoftwareTypeNode: ElementNode?
    get() = content.filterIsInstance<ElementNode>().singleOrNull()

fun DocumentNodeContainer.childElementNodesOfFunction(
    resolutionContainer: DocumentResolutionContainer,
    function: SchemaFunction
): List<ElementNode> =
    content.filterIsInstance<ElementNode>().filter {
        resolutionContainer.data(it)
            .ifSuccess()
            ?.elementFactoryFunction == function
    }

fun DocumentNodeContainer.property(name: String): PropertyNode? =
    content.filterIsInstance<PropertyNode>().singleOrNull { it.name == name }

internal fun ElementResolution.ifSuccess(): SuccessfulElementResolution? {
    return this as? SuccessfulElementResolution
}

internal fun PropertyResolution.ifSuccess(): PropertyAssignmentResolved? {
    return this as? PropertyAssignmentResolved
}
