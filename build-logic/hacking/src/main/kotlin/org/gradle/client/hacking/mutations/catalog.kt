package org.gradle.client.hacking.mutations

import org.gradle.internal.declarativedsl.dom.mutation.DefaultMutationDefinitionCatalog

val mutationCatalog = DefaultMutationDefinitionCatalog().apply {
    // Java
    registerMutationDefinition(SetJvmApplicationMainClass)
    registerMutationDefinition(SetJavaVersion)
}