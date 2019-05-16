/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.SimpleDiagnostic
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.findSerializableAnnotationDeclaration
import org.jetbrains.kotlinx.serialization.compiler.resolve.hasCompanionObjectAsSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.hasSerializableAnnotationWithoutArgs
import org.jetbrains.kotlinx.serialization.compiler.resolve.isInternalSerializable

class SerializationPluginDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        checkCanBeSerializedInternally(descriptor, context.trace)
    }

    private fun checkCanBeSerializedInternally(descriptor: DeclarationDescriptor, trace: BindingTrace) {
        if (descriptor !is ClassDescriptor) return
        if (!descriptor.hasSerializableAnnotationWithoutArgs) return

        if (!descriptor.isInternalSerializable && !descriptor.hasCompanionObjectAsSerializer) {
            trace.safeReport(descriptor.findSerializableAnnotationDeclaration()) {
                SerializationErrors.SERIALIZABLE_ANNOTATION_IGNORED.on(it)
            }
        }
    }

    private fun <E : PsiElement> BindingTrace.safeReport(element: E?, reporterFactory: (E) -> SimpleDiagnostic<E>) {
        element?.let { e ->
            reportFromPlugin(
                reporterFactory(e),
                SerializationPluginErrorsRendering
            )
        }
    }
}

object SerializationPluginErrorsRendering : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("SerializationPlugin")
    override fun getMap() = MAP

    init {
        MAP.put(
            SerializationErrors.SERIALIZABLE_ANNOTATION_IGNORED,
            "@Serializable annotation would be ignored because it is impossible to serialize automatically interfaces or enums. " +
                    "Provide serializer manually via e.g. companion object"
        )
    }
}
