package org.gradle.client.agentic

import org.gradle.declarative.dsl.schema.DataProperty
import org.gradle.declarative.dsl.schema.DataType
import org.gradle.declarative.dsl.schema.SchemaDocumentation
import org.gradle.declarative.dsl.schema.SchemaItemMetadata
import org.gradle.declarative.dsl.schema.SchemaMemberFunction

interface DocumentationProvider {
    fun classDocumentation(classDataType: DataType.ClassDataType): String?
    fun functionDocumentation(schemaMemberFunction: SchemaMemberFunction): String?
    fun propertyDocumentation(property: DataProperty): String?
}

object SchemaEmbeddedDocumentationProvider : DocumentationProvider {
    override fun classDocumentation(classDataType: DataType.ClassDataType): String? =
        extractDocsFromMetadata(classDataType.metadata)

    override fun functionDocumentation(schemaMemberFunction: SchemaMemberFunction): String? =
        extractDocsFromMetadata(schemaMemberFunction.metadata)

    override fun propertyDocumentation(property: DataProperty): String? =
        extractDocsFromMetadata(property.metadata)

    private fun extractDocsFromMetadata(metadata: Iterable<SchemaItemMetadata>) =
        metadata
            .filterIsInstance<SchemaDocumentation>()
            .flatMap {
                listOf(it.text) +
                    it.parts.map { (key, value) ->
                        "* `$key`: $value"
                    }
            }
            .joinToString("\n\n")
            .ifBlank { null }
}
