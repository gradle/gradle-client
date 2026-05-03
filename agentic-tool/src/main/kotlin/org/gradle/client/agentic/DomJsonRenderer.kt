package org.gradle.client.agentic

import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.gradle.client.core.gradle.dcl.userFriendlyErrorMessages
import org.gradle.declarative.dsl.schema.ConfigureFromGetterOrigin
import org.gradle.declarative.dsl.schema.ContainerElementFactory
import org.gradle.declarative.dsl.schema.DataClass
import org.gradle.declarative.dsl.schema.DataProperty
import org.gradle.declarative.dsl.schema.DataType
import org.gradle.declarative.dsl.schema.DataType.ParameterizedTypeInstance.TypeArgument.ConcreteTypeArgument
import org.gradle.declarative.dsl.schema.DataType.ParameterizedTypeInstance.TypeArgument.StarProjection
import org.gradle.declarative.dsl.schema.DataTypeRef
import org.gradle.declarative.dsl.schema.FunctionSemantics.ConfigureSemantics
import org.gradle.declarative.dsl.schema.ProjectFeatureOrigin
import org.gradle.declarative.dsl.schema.SchemaItemMetadata
import org.gradle.declarative.dsl.schema.SchemaMemberFunction
import org.gradle.declarative.dsl.schema.UnsafeSchemaItem
import org.gradle.internal.declarativedsl.analysis.ref
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.ElementNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.ErrorNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.PropertyNode
import org.gradle.internal.declarativedsl.dom.DocumentResolution
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ElementResolution.SuccessfulElementResolution
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ValueNodeResolution.ValueFactoryResolution.ValueFactoryResolved
import org.gradle.internal.declarativedsl.dom.operations.overlay.DocumentOverlayResult
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin
import org.gradle.internal.declarativedsl.language.SourceData
import kotlin.collections.forEach
import kotlin.collections.orEmpty

@Suppress("TooManyFunctions")
class DomJsonRenderer(
    val withUnusedMembers: Boolean,
    val dom: DocumentOverlayResult
) {
    @Suppress("ComplexMethod")
    fun JsonArrayBuilder.visitNode(node: DeclarativeDocument.DocumentNode) {
        when (node) {
            is ElementNode -> addJsonObject {
                val type = putElementHeaderAndGetType(node)

                val properties = node.content.filterIsInstance<PropertyNode>()
                val unusedProperties = (type as? DataClass)?.properties?.toSet()
                    ?.minus(properties.mapNotNull { it.asResolved(dom)?.property }.toSet())
                    .orEmpty()

                if (properties.isNotEmpty()) {
                    put("properties", buildJsonObject {
                        properties.forEach { putProperty(it) }
                    })
                }
                if (withUnusedMembers) {
                    put("unusedProperties", buildJsonObject {
                        unusedProperties.forEach { property -> putUnusedProperty(property) }
                    })
                }

                val (features, nestedObjects) =
                    node.content.filterIsInstance<ElementNode>().partition { isFeatureUsageElement(it) }

                if (features.isNotEmpty()) {
                    put("features", buildJsonArray { features.forEach { visitNode(it) } })
                }
                if (nestedObjects.isNotEmpty()) {
                    putJsonArray("nestedObjects") { nestedObjects.forEach { visitNode(it) } }
                }

                val usedFunctions = node.content.filterIsInstance<ElementNode>()
                    .map(dom.overlayResolutionContainer::data)
                    .filterIsInstance<SuccessfulElementResolution>()
                    .map { it.elementFactoryFunction }
                    .toSet()

                val unusedNestedObjects = (type as? DataClass)?.memberFunctions
                    ?.filter { it.semantics is ConfigureSemantics && it !in usedFunctions }
                    .orEmpty()

                if (withUnusedMembers) {
                    putJsonArray("unusedNestedObjects") {
                        unusedNestedObjects.forEach { putUnusedNestedObject(it) }
                    }
                }

                val errors = node.content.filterIsInstance<ErrorNode>()
                if (errors.isNotEmpty()) {
                    putJsonArray("errors") {
                        errors.forEach { putErrorNode(dom, it) }
                    }
                }
            }

            is PropertyNode -> Unit // no handling at top level
            is ErrorNode -> putErrorNode(dom, node)
        }
    }

    private fun isFeatureUsageElement(node1: ElementNode): Boolean =
        (dom.overlayResolutionContainer.data(node1) as? SuccessfulElementResolution)
            ?.elementFactoryFunction?.metadata?.any { it is ProjectFeatureOrigin } == true

    private fun JsonObjectBuilder.putElementHeaderAndGetType(
        node: ElementNode,
    ): DataType? {
        put("name", node.name)
        putJsonArray("args") {
            node.elementValues.forEach { putValueInline(it, dom) }
        }

        val overlayOrigin = dom.overlayNodeOriginContainer.data(node)
        val resolution = dom.overlayResolutionContainer.data(node)
        val function = when (resolution) {
            is SuccessfulElementResolution -> {
                resolution.elementFactoryFunction
            }

            is DocumentResolution.UnsuccessfulResolution -> {
                put("isUnresolved", true)
                putJsonArray("causes") {
                    userFriendlyErrorMessages(node, resolution).forEach(::add)
                }
                null
            }
        }
        val type = if (resolution is SuccessfulElementResolution) {
            resolution.elementType
        } else null

        if (type != null) {
            put("type", type.ref.typeName)
        }

        elementMetadata(function)

        sourceLocation(overlayOrigin, node)
        return type
    }

    fun JsonObjectBuilder.putProperty(property: PropertyNode) {
        val resolved = dom.overlayResolutionContainer.data(property)
        put(
            property.name, buildJsonObject {
                if (property.augmentation == PropertyNode.PropertyAugmentation.Plus) {
                    put("augmentedWithPlusAssign", true)
                }
                when (resolved) {
                    is DocumentResolution.PropertyResolution.PropertyAssignmentResolved -> {
                        put("type", resolved.property.valueType.typeName)
                    }

                    is DocumentResolution.PropertyResolution.PropertyNotAssigned -> {
                        put("isUnresolved", true)
                        putJsonArray("causes") {
                            userFriendlyErrorMessages(property, resolved).forEach(::add)
                        }
                    }
                }

                val origin = dom.overlayNodeOriginContainer.data(property)

                sourceLocation(origin, property)
                putValue(property.value, dom)
                if (origin is OverlayNodeOrigin.MergedProperties) {
                    val effective = origin.effectivePropertiesFromUnderlay +
                        origin.effectivePropertiesFromOverlay

                    if (effective.isNotEmpty()) {
                        putJsonArray("multipartEffectiveValue") {
                            effective.forEach { putValueInline(it.value, dom) }
                        }
                    }

                    val shadowed = origin.shadowedPropertiesFromUnderlay +
                        origin.shadowedPropertiesFromOverlay

                    if (shadowed.isNotEmpty()) {
                        putJsonArray("overridesValues") {
                            shadowed.forEach { putValueInline(it.value, dom) }
                        }
                    }
                }
            })
    }

    private fun JsonObjectBuilder.putUnusedProperty(property: DataProperty) {
        put(property.name, buildJsonObject {
            put("type", property.valueType.typeName)
        })
    }

    private fun JsonArrayBuilder.putUnusedNestedObject(function: SchemaMemberFunction) {
        addJsonObject {
            put("name", function.simpleName)
            put("type", (function.semantics as ConfigureSemantics).configuredType.typeName)
            put(
                "isBlock",
                (function.semantics as ConfigureSemantics).configureBlockRequirement.requires
            )
            if (function.parameters.isNotEmpty()) {
                putJsonArray("parameters") {
                    function.parameters.forEach { param ->
                        addJsonObject {
                            put("name", param.name)
                            put("type", param.type.typeName)
                        }
                    }
                }
            }
        }
    }

    private fun JsonArrayBuilder.putErrorNode(
        fullDom: DocumentOverlayResult,
        node: ErrorNode
    ) {
        addJsonObject {
            val errorResolution = fullDom.overlayResolutionContainer.data(node)
            put("source", node.sourceData.text())
            sourceLocation(fullDom.overlayNodeOriginContainer.data(node), node)
            putJsonArray("causes") {
                userFriendlyErrorMessages(node, errorResolution).forEach(::add)
            }
        }
    }

    private fun JsonArrayBuilder.putValueInline(
        node: DeclarativeDocument.ValueNode,
        fullDom: DocumentOverlayResult
    ) {
        when (node) {
            is DeclarativeDocument.ValueNode.LiteralValueNode -> add(node.sourceData.text())
            is DeclarativeDocument.ValueNode.NamedReferenceNode -> add(node.referenceName)
            is DeclarativeDocument.ValueNode.ValueFactoryNode -> addJsonObject { putValue(node, fullDom) }
        }
    }

    private fun JsonObjectBuilder.putValue(
        node: DeclarativeDocument.ValueNode,
        fullDom: DocumentOverlayResult
    ) {
        when (node) {
            is DeclarativeDocument.ValueNode.LiteralValueNode -> put("value", node.sourceData.text())
            is DeclarativeDocument.ValueNode.NamedReferenceNode -> {
                put("value", node.referenceName)
            }

            is DeclarativeDocument.ValueNode.ValueFactoryNode -> putJsonObject("value") {
                put("source", node.sourceData.text())
                val resolved = fullDom.overlayResolutionContainer.data(node)
                if (resolved is ValueFactoryResolved) {
                    put("type", resolved.function.semantics.returnValueType.typeName)
                }
                put("valueFactory", node.factoryName + (if (node.isInfix) "(infix-notation)" else ""))
                putJsonArray("args") {
                    node.values.forEach {
                        putValueInline(it, fullDom)
                    }
                }
            }
        }
    }

    private fun JsonObjectBuilder.sourceLocation(
        overlayOrigin: OverlayNodeOrigin,
        node: DeclarativeDocument.DocumentNode
    ) {
        when (overlayOrigin) {
            is OverlayNodeOrigin.CopiedOrigin -> {
                if (overlayOrigin is OverlayNodeOrigin.FromUnderlay) {
                    put("isDefault", true)
                }
                put("sourceLocation", node.sourceData.locationString)
            }

            is OverlayNodeOrigin.MergedElements -> putJsonArray("sourceLocations") {
                add(overlayOrigin.underlayElement.sourceData.locationString)
                add(overlayOrigin.overlayElement.sourceData.locationString)
            }

            is OverlayNodeOrigin.MergedProperties -> putJsonArray("sourceLocations") {
                overlayOrigin.effectivePropertiesFromUnderlay.map { it.sourceData.locationString }.forEach(::add)
                overlayOrigin.effectivePropertiesFromOverlay.map { it.sourceData.locationString }.forEach(::add)
            }
        }
    }

    private fun PropertyNode.asResolved(inDom: DocumentOverlayResult) =
        inDom.overlayResolutionContainer.data(this) as? DocumentResolution.PropertyResolution.PropertyAssignmentResolved


    private val SourceData.locationString: String
        get() =
            "${this.sourceIdentifier.fileIdentifier}:${this.lineRange.first}"
}

internal fun JsonObjectBuilder.elementMetadata(function: SchemaMemberFunction?) =
    function?.metadata?.let(::elementMetadata)

internal fun JsonObjectBuilder.elementMetadata(metadata: Iterable<SchemaItemMetadata>) {
    metadata.forEach { metadata ->
        when (metadata) {
            is ProjectFeatureOrigin -> {
                val isProjectType =
                    metadata.targetDefinitionClassName == "org.gradle.api.Project"
                putJsonObject(if (isProjectType) "projectType" else "feature") {
                    put("name", metadata.featureName)
                    put("ecosystemPluginId", metadata.ecosystemPluginId)
                }
            }

            is ConfigureFromGetterOrigin -> Unit
            is ContainerElementFactory -> Unit
            is UnsafeSchemaItem -> Unit
        }
    }
}

internal val DataTypeRef.typeName: String
    get() = when (this) {
        is DataTypeRef.Name -> this.fqName.qualifiedName
        is DataTypeRef.NameWithArgs -> this.fqName.qualifiedName + "<${
            typeArguments.joinToString { arg ->
                when (arg) {
                    is ConcreteTypeArgument -> {
                        arg.type.typeName
                    }

                    is StarProjection -> "*"
                }
            }
        }>"

        is DataTypeRef.Type -> when (val t = dataType) {
            is DataType.BooleanDataType -> "Boolean"
            is DataType.IntDataType -> "Int"
            is DataType.LongDataType -> "Long"
            is DataType.StringDataType -> "String"
            is DataType.NullType -> "Nothing?"
            is DataType.TypeVariableUsage -> "TypeVariable#${t.variableId}"
            is DataType.UnitType -> "Unit"
        }
    }
