package org.gradle.client.agentic

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.serialization.json.*
import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.declarative.dsl.schema.*
import org.gradle.declarative.dsl.schema.DataType.ClassDataType
import org.gradle.declarative.dsl.schema.DataType.ParameterizedTypeInstance.TypeArgument.ConcreteTypeArgument
import org.gradle.declarative.dsl.schema.FunctionSemantics.ConfigureSemantics
import org.gradle.internal.declarativedsl.analysis.SchemaTypeRefContext
import org.gradle.internal.declarativedsl.analysis.isReadOnly
import org.gradle.internal.declarativedsl.evaluator.main.SimpleAnalysisEvaluator

class ProjectSchemaCommand : DclCommand("project-schema") {
    override fun help(context: Context): String =
        "Prints the schema for the project types and features available in the build. " +
            "If project features are specified, only their schema will be returned. " +
            "If a subproject path is specified, only the schema for that subproject's " +
            "project type (and its features) will be returned."

    val projectFeatures by option(
        help = "Comma-separated project type or feature names for which to query settings defaults; " +
            "e.g. javaLibrary,javaApplication,spotless" +
            "If not specified, returns the schema for all project types and features"
    )

    val subprojectPath by option(
        help = "Gradle project path such as :foo:bar or filesystem path such as foo/bar"
    )

    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    override fun produceOutput(
        prerequisites: ResolvedDomPrerequisites,
        analyzer: SimpleAnalysisEvaluator
    ): JsonElement {
        if (projectFeatures != null && subprojectPath != null) {
            return buildJsonObject { put("error", "Subproject path and project features cannot be specified together") }
        }

        val schema =
            prerequisites.projectInterpretationSequence.steps.last().evaluationSchemaForStep.analysisSchema

        val projectFeaturePredicate: ((ProjectFeatureOrigin) -> Boolean)? = when {
            projectFeatures != null -> {
                val featureNames = splitCommaSeparatedFeatureNames(projectFeatures);
                { origin -> featureNames?.contains(origin.featureName) ?: true }
            }

            subprojectPath != null -> {
                val projectInfo =
                    matchingProjectInfo(prerequisites, subprojectPath!!)
                        ?: return buildJsonObject { put("error", "Project not found for path: $subprojectPath") }

                val dom = getDomForProject(analyzer, prerequisites, projectInfo)
                    ?: return buildJsonObject { put("error", "Failed to analyze project at path: $subprojectPath") }

                val used = collectProjectTypeAndFeatures(dom).run { projectFeatures + listOfNotNull(projectType) };
                { feature -> used.any(feature::matchesFeature) }
            }

            else -> null
        }

        val initialTypes = if (projectFeaturePredicate != null)
            matchingInitialTypes(schema, projectFeaturePredicate)
        else listOf(schema.topLevelReceiverType)

        val allUsedTypes = collectTypesToInclude(schema, initialTypes)

        return buildJsonObject {
            if (projectFeaturePredicate == null) {
                put("topLevelTypeName", schema.topLevelReceiverType.name.qualifiedName)
            }

            put("types", buildJsonArray {
                allUsedTypes.forEach { type ->
                    add(typeInfo(type))
                }
            })
        }
    }

    private fun matchingInitialTypes(
        schema: AnalysisSchema,
        projectFeaturePredicate: (ProjectFeatureOrigin) -> Boolean
    ): List<DataClass> = schema.dataClassTypesByFqName.values.filterIsInstance<DataClass>().filter { type ->
        type.metadata.any { metadata ->
            metadata is ProjectFeatureOrigin && projectFeaturePredicate(metadata)
        }
    }

    private fun typeInfo(type: ClassDataType): JsonObject = buildJsonObject {
        if (type is EnumClass) {
            put("name", type.name.qualifiedName)
            putJsonArray("enumEntries") {
                type.entryNames.forEach { add(it) }
            }
        }
        if (type is DataClass) {
            put("name", type.name.qualifiedName)
            val supertypes = type.supertypes.filter { it.qualifiedName != Any::class.qualifiedName }
            if (supertypes.isNotEmpty()) {
                putJsonArray("supertypes") {
                    type.supertypes.forEach { add(it.qualifiedName) }
                }
            }
            if (type.properties.isNotEmpty()) {
                put("properties", buildJsonArray {
                    type.properties.forEach { add(propertyInfo(it)) }
                })
            }

            elementMetadata(type.metadata)

            val (features, nestedObjectFunctions) = type.memberFunctions
                .filter { it.semantics is ConfigureSemantics }
                .partition { it.metadata.filterIsInstance<ProjectFeatureOrigin>().any() }

            if (features.isNotEmpty()) {
                put("features", buildJsonArray {
                    features.forEach { add(nestedObjectInfo(it)) }
                })
            }
            if (nestedObjectFunctions.isNotEmpty()) {
                put("nestedObjects", buildJsonArray {
                    nestedObjectFunctions.forEach { add(nestedObjectInfo(it)) }
                })
            }
            val valueFactories = type.memberFunctions.filter { it.semantics is FunctionSemantics.Pure }
            if (valueFactories.isNotEmpty()) {
                put("valueFactories", buildJsonArray {
                    valueFactories.forEach { add(valueFactoryInfo(it)) }
                })
            }
        }

        documentationProvider.classDocumentation(type.name.qualifiedName)?.let { docs ->
            put("documentation", docs)
        }
    }

    private fun propertyInfo(property: DataProperty): JsonObject = buildJsonObject {
        put("name", property.name)
        put("type", property.valueType.typeName)
        if (property.isReadOnly) {
            put("isReadOnly", true)
        }
    }


    private fun nestedObjectInfo(function: SchemaMemberFunction): JsonObject = buildJsonObject {
        put("name", function.simpleName)
        elementMetadata(function)
        putJsonArray("parameters") {
            function.parameters.forEach { parameter ->
                addJsonObject {
                    put("name", parameter.name)
                    put("type", parameter.type.typeName)
                    when (parameter.semantics) {
                        is ParameterSemantics.IdentityKey -> put("semantics", "identity-key")
                        else -> Unit
                    }
                }
            }
        }
        val semantics = function.semantics as ConfigureSemantics
        put("type", (function.semantics as ConfigureSemantics).configuredType.typeName)
        when (semantics) {
            is FunctionSemantics.AccessAndConfigure -> {
                put(
                    "semantics",
                    when {
                        isFeatureUsageFunction(function) -> "feature-application"
                        function.parameters.any { it.semantics is ParameterSemantics.IdentityKey } ->
                            "creates-or-configures-object-by-id"
                        else -> "pre-existing-nested-object"
                    }
                )
            }

            is FunctionSemantics.AddAndConfigure ->
                put("semantics", "adds-new-object")
        }
    }

    private fun collectTypesToInclude(
        schema: AnalysisSchema,
        initial: Iterable<DataClass>
    ): Set<ClassDataType> = with(SchemaTypeRefContext(schema)) {

        buildSet {
            fun visitType(type: DataClass) {
                fun maybeVisitRef(ref: DataTypeRef) {
                    when (val type = resolveRef(ref)) {
                        is DataClass -> {
                            visitType(type)
                        }

                        is DataType.ParameterizedTypeInstance -> {
                            type.typeArguments.forEach { if (it is ConcreteTypeArgument) maybeVisitRef(it.type) }
                        }

                        is EnumClass -> add(type)
                        is DataType.PrimitiveType -> Unit
                    }
                }

                if (add(type)) {
                    type.properties.forEach {
                        maybeVisitRef(it.valueType)
                    }
                    type.memberFunctions.forEach { function ->
                        function.parameters.forEach { maybeVisitRef(it.type) }
                        val semantics = function.semantics
                        maybeVisitRef(semantics.returnValueType)
                        if (semantics is ConfigureSemantics) {
                            maybeVisitRef(semantics.configuredType)
                        }
                    }
                }
            }

            initial.forEach(::visitType)
        }
    }

    private fun valueFactoryInfo(function: SchemaMemberFunction): JsonObject = buildJsonObject {
        put("name", function.simpleName)
        put("returnType", (function.semantics as FunctionSemantics.Pure).returnValueType.typeName)
        putJsonArray("parameters") {
            function.parameters.forEach { parameter ->
                addJsonObject {
                    put("name", parameter.name)
                    put("type", parameter.type.typeName)
                }
            }
        }
    }
}

private fun ProjectFeatureOrigin.matchesFeature(
    feature: ProjectFeatureOrigin
): Boolean = featureName == feature.featureName &&
    targetDefinitionClassName == feature.targetDefinitionClassName &&
    targetBuildModelClassName == feature.targetBuildModelClassName
