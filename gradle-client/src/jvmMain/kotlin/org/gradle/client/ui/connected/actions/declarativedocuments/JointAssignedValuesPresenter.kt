package org.gradle.client.ui.connected.actions.declarativedocuments

import org.gradle.declarative.dsl.schema.DataTopLevelFunction
import org.gradle.declarative.dsl.schema.DataType
import org.gradle.internal.declarativedsl.analysis.TypeRefContext
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.PropertyNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.ValueNode
import org.gradle.internal.declarativedsl.dom.DocumentResolution.PropertyResolution.PropertyAssignmentResolved
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ValueNodeResolution.ValueFactoryResolution.ValueFactoryResolved
import org.gradle.internal.declarativedsl.dom.resolution.DocumentResolutionContainer

internal interface JointAssignedValuesPresenter {
    fun supportsPropertyType(propertyType: DataType): Boolean

    fun jointAssignedValuesPresentation(
        effectivePropertyNodes: List<PropertyNode>,
        resolutionContainer: DocumentResolutionContainer
    ): Map<PropertyNode, AssignedValuePresentation>
}

internal fun findValuePresenter(
    resolutionContainer: DocumentResolutionContainer,
    typeRefContext: TypeRefContext,
    representativeNode: PropertyNode?
): JointAssignedValuesPresenter? =
    representativeNode?.run {
        val propertyTypeRef = (resolutionContainer.data(this) as? PropertyAssignmentResolved)?.property?.valueType
        if (propertyTypeRef != null) {
            val propertyType = typeRefContext.resolveRef(propertyTypeRef)
            supportedJointAssignedValuePresenters.find { it.supportsPropertyType(propertyType) }
        } else null
    }

internal class AssignedValuePresentation(
    val valueNodeEntries: List<ValueNodeEntry>
)

internal sealed interface ValueNodeEntry {
    val fullValueNode: ValueNode
    val isEffectiveItem: Boolean
}

internal data class MonolithicValueNodeEntry(
    override val fullValueNode: ValueNode,
    override val isEffectiveItem: Boolean
) : ValueNodeEntry

internal data class KeyValueNodeEntry(
    override val fullValueNode: ValueNode,
    val keyNode: ValueNode,
    val valueNode: ValueNode,
    override val isEffectiveItem: Boolean
) : ValueNodeEntry

internal val supportedJointAssignedValuePresenters = listOf(
    ListAssignedValuesPresenter(),
    MapAssignedValuesPresenter()
)

internal class MapAssignedValuesPresenter : JointAssignedValuesPresenter {
    override fun supportsPropertyType(propertyType: DataType) =
        propertyType is DataType.ParameterizedTypeInstance &&
            propertyType.name.qualifiedName == Map::class.qualifiedName

    override fun jointAssignedValuesPresentation(
        effectivePropertyNodes: List<PropertyNode>,
        resolutionContainer: DocumentResolutionContainer
    ): Map<PropertyNode, AssignedValuePresentation> {
        val presentKeys = mutableSetOf<Any>()

        return effectivePropertyNodes.asReversed().mapNotNull { propertyNode ->
            val value = propertyNode.value
            if (value !is ValueNode.ValueFactoryNode) return@mapNotNull null

            val resolution = resolutionContainer.data(value)
            when (resolution) {
                is ValueFactoryResolved -> {
                    if (resolution.function.run {
                            this is DataTopLevelFunction && packageName == "kotlin.collections" && simpleName == "mapOf"
                        }) {
                        val entries = value.values.asReversed()
                        propertyNode to AssignedValuePresentation(entries.map { entry ->
                            val isPairProducedByTo = entry is ValueNode.ValueFactoryNode &&
                                (resolutionContainer.data(entry) as? ValueFactoryResolved)?.function.run {
                                    this is DataTopLevelFunction && simpleName == "to" && packageName == "kotlin"
                                }
                            if (isPairProducedByTo) {
                                val key = entry.values.first()
                                val value = entry.values.last()
                                val isEffective = key !is ValueNode.LiteralValueNode || presentKeys.add(key.value)
                                KeyValueNodeEntry(entry, key, value, isEffective)
                            } else MonolithicValueNodeEntry(entry, isEffectiveItem = true)
                        }.asReversed())
                    } else null
                }

                else -> null
            }
        }.asReversed().toMap()
    }
}


internal class ListAssignedValuesPresenter : JointAssignedValuesPresenter {
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
                            valueNodeEntries = value.values.map { MonolithicValueNodeEntry(it, isEffectiveItem = true) }
                        )
                    } else null
                }

                else -> null
            }
        }.toMap()
}
