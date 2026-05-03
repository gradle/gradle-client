package org.gradle.client.agentic

interface DocumentationProvider {
    fun classDocumentation(classFqName: String): String?

    // todo: use a member signature to disambiguate function overloads
    fun memberDocumentation(classFqName: String, memberName: String): String?
}

object NoopDocumentationProvider : DocumentationProvider {
    override fun classDocumentation(classFqName: String): String? = null
    override fun memberDocumentation(classFqName: String, memberName: String): String? = null
}
