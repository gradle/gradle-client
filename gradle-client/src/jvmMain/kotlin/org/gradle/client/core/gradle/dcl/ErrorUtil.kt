package org.gradle.client.core.gradle.dcl

import org.gradle.internal.declarativedsl.dom.AmbiguousName
import org.gradle.internal.declarativedsl.dom.BlockMismatch
import org.gradle.internal.declarativedsl.dom.CrossScopeAccess
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.DocumentResolution
import org.gradle.internal.declarativedsl.dom.IllegalAugmentedAssignment
import org.gradle.internal.declarativedsl.dom.IsError
import org.gradle.internal.declarativedsl.dom.NonEnumValueNamedReference
import org.gradle.internal.declarativedsl.dom.NotAssignable
import org.gradle.internal.declarativedsl.dom.OpaqueValueInIdentityKey
import org.gradle.internal.declarativedsl.dom.SyntaxError
import org.gradle.internal.declarativedsl.dom.UnresolvedBase
import org.gradle.internal.declarativedsl.dom.UnresolvedName
import org.gradle.internal.declarativedsl.dom.UnresolvedSignature
import org.gradle.internal.declarativedsl.dom.UnresolvedValueUsed
import org.gradle.internal.declarativedsl.dom.UnsupportedKotlinFeature
import org.gradle.internal.declarativedsl.dom.UnsupportedSyntax
import org.gradle.internal.declarativedsl.dom.ValueTypeMismatch

@Suppress("CyclomaticComplexMethod")
fun userFriendlyErrorMessages(
    node: DeclarativeDocument.Node,
    unsuccessfulResolution: DocumentResolution.UnsuccessfulResolution
): List<String> = unsuccessfulResolution.reasons.flatMap { reason ->
    when (reason) {
        AmbiguousName -> listOf("Ambiguous name")
        BlockMismatch -> if (node is DeclarativeDocument.DocumentNode.ElementNode) {
            if (node.content.isEmpty())
                listOf("Block expected for element ${node.name}")
            else listOf("Block not expected for element ${node.name}")
        } else listOf("Block mismatch")

        CrossScopeAccess -> listOf("Cross-scope access")
        
        IsError -> if (node is DeclarativeDocument.DocumentNode.ErrorNode) {
            node.errors.map { error ->
                when (error) {
                    is SyntaxError -> "Syntax error: ${error.parsingError.message}"
                    is UnsupportedKotlinFeature ->
                        "Unsupported language feature: ${error.unsupportedConstruct.languageFeature}"
                    is UnsupportedSyntax -> "Unsupported syntax: ${error.cause}"
                }
            }
        } else listOf("Syntax error")
        
        OpaqueValueInIdentityKey -> listOf("Opaque value passed as identity key")
        UnresolvedName -> listOf("Unresolved name")
        UnresolvedSignature -> listOf("Unresolved function signature")
        IllegalAugmentedAssignment -> listOf("Illegal augmented assignment")
        NotAssignable -> listOf("Cannot assign property")
        UnresolvedValueUsed -> listOf("Assigned value is not resolved")
        ValueTypeMismatch -> listOf("Value type mismatch")
        NonEnumValueNamedReference -> listOf("Illegal named reference. Only enum entries can be referenced by name")
        
        UnresolvedBase -> emptyList<String>()
    }
}