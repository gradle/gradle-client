package org.gradle.client.agentic

import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObject
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
import org.gradle.internal.declarativedsl.analysis.isReadOnly
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
    val dom: DocumentOverlayResult,
    val documentationProvider: DocumentationProvider,
) {
    @Suppress("ComplexMethod")
    fun JsonArrayBuilder.visitNode(inClass: DataClass?, node: DeclarativeDocument.DocumentNode) {
        when (node) {
            is ElementNode -> addJsonObject {
                val type = putElementHeaderAndGetType(node) as? DataClass

                val properties = node.content.filterIsInstance<PropertyNode>()
                val unusedProperties = type?.properties?.toSet()
                    ?.minus(properties.mapNotNull { it.asResolved(dom)?.property }.toSet())
                    .orEmpty()

                if (properties.isNotEmpty()) {
                    put("properties", buildJsonObject {
                        properties.forEach { put(it.name, propertyInfo(type, it)) }
                    })
                }
                if (withUnusedMembers && unusedProperties.isNotEmpty()) {
                    put("unusedProperties", buildJsonObject {
                        unusedProperties.forEach { put(it.name, unusedPropertyInfo(type!!, it)) }
                    })
                }

                val (features, nestedObjects) =
                    node.content.filterIsInstance<ElementNode>().partition { isFeatureUsageElement(it) }

                if (features.isNotEmpty()) {
                    put("features", buildJsonArray { features.forEach { visitNode(type, it) } })
                }
                if (nestedObjects.isNotEmpty()) {
                    putJsonArray("nestedObjects") { nestedObjects.forEach { visitNode(type, it) } }
                }

                val usedFunctions = node.content.asSequence()
                    .filterIsInstance<ElementNode>()
                    .map(dom.overlayResolutionContainer::data)
                    .filterIsInstance<SuccessfulElementResolution>()
                    .map { it.elementFactoryFunction }
                    .toSet()

                val (unusedFeatures, unusedNestedObjects) = type?.memberFunctions
                    ?.filter { it.semantics is ConfigureSemantics && it !in usedFunctions }
                    .orEmpty()
                    .partition { isFeatureUsageFunction(it) }

                if (withUnusedMembers && unusedFeatures.isNotEmpty()) {
                    putJsonArray("unusedFeatures") {
                        unusedFeatures.forEach { add(unusedNestedObject(type!!, it)) }
                    }
                }
                if (withUnusedMembers && unusedNestedObjects.isNotEmpty()) {
                    putJsonArray("unusedNestedObjects") {
                        unusedNestedObjects.forEach { add(unusedNestedObject(type!!, it)) }
                    }
                }

                val errors = node.content.filterIsInstance<ErrorNode>()
                if (errors.isNotEmpty()) {
                    putJsonArray("errors") {
                        errors.forEach { add(errorNode(dom, it)) }
                    }
                }

                if (inClass != null) {
                    documentationProvider.memberDocumentation(inClass.name.qualifiedName, node.name)
                        ?.let { put("documentation", it) }
                }
            }

            is PropertyNode -> add(propertyInfo(inClass, node))
            is ErrorNode -> add(errorNode(dom, node))
        }
    }

    private fun isFeatureUsageElement(node: ElementNode): Boolean =
        (dom.overlayResolutionContainer.data(node) as? SuccessfulElementResolution)
            ?.elementFactoryFunction?.let(::isFeatureUsageFunction) == true

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
            if (type is DataClass && type.supertypes.any { it.qualifiedName != Any::class.qualifiedName }) {
                putJsonArray("supertypes") {
                    type.supertypes.forEach { add(it.qualifiedName) }
                }
            }
            if (type is DataType.ClassDataType) {
                documentationProvider.classDocumentation(type.name.qualifiedName)
                    ?.let { put("typeDocumentation", it) }
            }
        }

        elementMetadata(function)

        sourceLocation(overlayOrigin, node)

        return type
    }

    fun propertyInfo(inType: DataClass?, property: PropertyNode): JsonObject {
        val resolved = dom.overlayResolutionContainer.data(property)
        return buildJsonObject {
            if (property.augmentation == PropertyNode.PropertyAugmentation.Plus) {
                put("augmentedWithPlusAssign", true)
            }
            when (resolved) {
                is DocumentResolution.PropertyResolution.PropertyAssignmentResolved -> {
                    put("type", resolved.property.valueType.typeName)
                    if (resolved.property.isReadOnly) {
                        put("isReadOnly", true)
                    }
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

                if (effective.size > 1) {
                    putJsonArray("multipartEffectiveValue") {
                        addJsonObject {
                            effective.forEach { putValue(it.value, dom) }
                        }
                    }
                }

                val shadowed = origin.shadowedPropertiesFromUnderlay +
                    origin.shadowedPropertiesFromOverlay

                if (shadowed.isNotEmpty()) {
                    putJsonArray("overridesValues") {
                        addJsonObject {
                            shadowed.forEach { putValue(it.value, dom) }
                        }
                    }
                }
            }

            if (inType != null) {
                documentationProvider.memberDocumentation(inType.name.qualifiedName, property.name)
                    ?.let { put("documentation", it) }
            }
        }
    }

    private fun unusedPropertyInfo(type: DataClass?, property: DataProperty): JsonObject = buildJsonObject {
        put("type", property.valueType.typeName)
        if (type != null) {
            documentationProvider.memberDocumentation(type.name.qualifiedName, property.name)
                ?.let { put("documentation", it) }
        }
    }

    private fun unusedNestedObject(type: DataClass, function: SchemaMemberFunction): JsonObject = buildJsonObject {
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
        documentationProvider.memberDocumentation(type.name.qualifiedName, function.simpleName)
            ?.let { put("documentation", it) }
    }

    private fun errorNode(
        fullDom: DocumentOverlayResult,
        node: ErrorNode
    ) = buildJsonObject {
        val errorResolution = fullDom.overlayResolutionContainer.data(node)
        put("source", node.sourceData.text())
        sourceLocation(fullDom.overlayNodeOriginContainer.data(node), node)
        putJsonArray("causes") {
            userFriendlyErrorMessages(node, errorResolution).forEach(::add)
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
            is DeclarativeDocument.ValueNode.LiteralValueNode -> {
                put("value", node.sourceData.text())
                sourceLocation(fullDom.overlayNodeOriginContainer.data(node), node)
            }
            is DeclarativeDocument.ValueNode.NamedReferenceNode -> {
                put("value", node.referenceName)
                sourceLocation(fullDom.overlayNodeOriginContainer.data(node), node)
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
        node: DeclarativeDocument.Node
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
        get() = "${this.sourceIdentifier.fileIdentifier}:${this.lineRange.first}"
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

internal fun isFeatureUsageFunction(function: SchemaMemberFunction): Boolean =
    function.metadata.any { it is ProjectFeatureOrigin }
