package org.gradle.client.ui.connected.actions

import org.gradle.declarative.dsl.schema.*

fun AnalysisSchema.dataClassFor(typeRef: DataTypeRef.Name): DataClass =
    dataClass(typeRef.fqName)

fun AnalysisSchema.dataClassFor(typeRef: DataTypeRef.NameWithArgs): DataClass =
    dataClass(typeRef.fqName)

private fun AnalysisSchema.dataClass(fqName: FqName) =
    (dataClassTypesByFqName.getValue(fqName) as? DataClass
        ?: error("unexpected type for $fqName"))

val AnalysisSchema.softwareTypes: List<SchemaMemberFunction>
    get() = topLevelReceiverType.memberFunctions

fun AnalysisSchema.softwareTypeNamed(name: String): SchemaMemberFunction? =
    softwareTypes.singleOrNull { it.simpleName == name }

val SchemaMemberFunction.softwareTypeSemantics: FunctionSemantics.AccessAndConfigure
    get() = semantics as FunctionSemantics.AccessAndConfigure

fun AnalysisSchema.configuredTypeOf(semantics: FunctionSemantics.ConfigureSemantics): DataClass =
    dataClassFor(semantics.configuredType as DataTypeRef.Name)

val DataProperty.typeName: String
    get() = when (val propType = valueType) {
        is DataTypeRef.Type -> propType.dataType.toString()
        is DataTypeRef.Name -> propType.toHumanReadable()
        is DataTypeRef.NameWithArgs -> propType.toHumanReadable()
    }

fun DataTypeRef.toHumanReadable(): String =
    when (this) {
        is DataTypeRef.Name -> fqName.simpleName
        is DataTypeRef.Type -> toHumanReadable()
        is DataTypeRef.NameWithArgs -> "${fqName.simpleName}<${typeArguments.joinToString()}>"
    }

fun DataTypeRef.Type.toHumanReadable(): String =
    when (val type = this.dataType) {
        is DataType.NullType -> "null"
        is DataType.UnitType -> Unit::class.simpleName!!
        is DataType.ConstantType<*> -> type.toString()
        is DataType.ClassDataType -> type.name.simpleName
        is DataType.TypeVariableUsage -> "generic-type-${type.variableId}"
    }

fun DataParameter.toHumanReadable(): String =
    type.toHumanReadable()

val List<SchemaMemberFunction>.accessAndConfigure: List<SchemaMemberFunction>
    get() = filter { it.semantics is FunctionSemantics.AccessAndConfigure }

val SchemaMemberFunction.accessAndConfigureSemantics: FunctionSemantics.AccessAndConfigure
    get() = semantics as FunctionSemantics.AccessAndConfigure

val List<SchemaMemberFunction>.addAndConfigure: List<SchemaMemberFunction>
    get() = filter { it.semantics is FunctionSemantics.AddAndConfigure }

val SchemaMemberFunction.addAndConfigureSemantics: FunctionSemantics.AddAndConfigure
    get() = semantics as FunctionSemantics.AddAndConfigure
