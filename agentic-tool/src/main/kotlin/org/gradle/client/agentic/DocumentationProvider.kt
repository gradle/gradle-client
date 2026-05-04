package org.gradle.client.agentic

interface DocumentationProvider {
    fun classDocumentation(classFqName: String): String?

    // todo: use a member signature to disambiguate function overloads
    fun memberDocumentation(classFqName: String, memberName: String): String?

    fun featureDocumentation(pluginId: String?, featureName: String): String?
}

object NoopDocumentationProvider : DocumentationProvider {
    override fun featureDocumentation(pluginId: String?, featureName: String): String? = null
    override fun classDocumentation(classFqName: String): String? = null
    override fun memberDocumentation(classFqName: String, memberName: String): String? = null
}
