package org.gradle.client.ui.connected.actions.declarativedocuments

import org.gradle.declarative.dsl.evaluation.InterpretationSequence
import org.gradle.declarative.dsl.schema.DataType
import org.gradle.declarative.dsl.schema.DataTypeRef
import org.gradle.internal.declarativedsl.analysis.SchemaTypeRefContext
import org.gradle.internal.declarativedsl.analysis.TypeRefContext

class JointTypeRefContext(sequences: List<InterpretationSequence>): TypeRefContext {
    private val typeRefContexts =
        sequences.flatMap { it.steps }.map { SchemaTypeRefContext(it.evaluationSchemaForStep.analysisSchema) }

    override fun maybeResolveRef(dataTypeRef: DataTypeRef): DataType? =
        typeRefContexts.firstNotNullOfOrNull { it.maybeResolveRef(dataTypeRef) }

    override fun resolveRef(dataTypeRef: DataTypeRef): DataType =
        typeRefContexts.firstNotNullOfOrNull { it.maybeResolveRef(dataTypeRef) }
            ?: error("failed to resolve a type reference to $dataTypeRef")
}
