package org.gradle.client.ui.connected.actions.declarativedocuments

import org.gradle.declarative.dsl.schema.DataTopLevelFunction
import org.gradle.declarative.dsl.schema.DataType
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.PropertyNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.ValueNode
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ValueNodeResolution.ValueFactoryResolution.ValueFactoryResolved
import org.gradle.internal.declarativedsl.dom.resolution.DocumentResolutionContainer

interface JointAssignedValuesPresenter {
    fun supportsPropertyType(propertyType: DataType): Boolean

    fun jointAssignedValuesPresentation(
        effectivePropertyNodes: List<PropertyNode>,
        resolutionContainer: DocumentResolutionContainer
    ): Map<PropertyNode, AssignedValuePresentation>
}

class AssignedValuePresentation(
    val effectiveValueNodes: List<ValueNode>
)

val supportedJointAssignedValuePresentaters = listOf(
    ListAssignedValuesPresenter()
)

class ListAssignedValuesPresenter : JointAssignedValuesPresenter {
    override fun supportsPropertyType(propertyType: DataType) =
        propertyType is DataType.ParameterizedTypeInstance &&
            propertyType.name.qualifiedName == List::class.qualifiedName

    override fun jointAssignedValuesPresentation(
        effectivePropertyNodes: List<PropertyNode>,
        resolutionContainer: DocumentResolutionContainer
    ): Map<PropertyNode, AssignedValuePresentation> =
        effectivePropertyNodes.mapNotNull { propertyNode ->
            val value = propertyNode.value
            if (value !is ValueNode.ValueFactoryNode) return@mapNotNull null
            val resolution = resolutionContainer.data(value)
            when (resolution) {
                is ValueFactoryResolved -> {
                    if (resolution.function.run {
                            this is DataTopLevelFunction &&
                                packageName == "kotlin.collections" &&
                                simpleName == "listOf"
                        }) {
                        propertyNode to AssignedValuePresentation(
                            effectiveValueNodes = value.values
                        )
                    } else null
                }

                else -> null
            }
        }.toMap()
}
