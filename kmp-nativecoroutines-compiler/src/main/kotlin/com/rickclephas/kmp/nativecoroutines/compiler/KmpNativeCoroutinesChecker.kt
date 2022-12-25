package com.rickclephas.kmp.nativecoroutines.compiler

import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.CONFLICT_COROUTINES_STATE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.EXPOSED_FLOW_TYPE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.EXPOSED_SUSPEND_FUNCTION
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.IGNORED_COROUTINES
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.IGNORED_COROUTINES_STATE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.INVALID_COROUTINES
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.INVALID_COROUTINES_IGNORE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.INVALID_COROUTINE_SCOPE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.INVALID_COROUTINES_STATE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.REDUNDANT_OVERRIDE_COROUTINES
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.REDUNDANT_OVERRIDE_COROUTINES_IGNORE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.REDUNDANT_OVERRIDE_COROUTINES_STATE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.REDUNDANT_PRIVATE_COROUTINES
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.REDUNDANT_PRIVATE_COROUTINES_IGNORE
import com.rickclephas.kmp.nativecoroutines.compiler.KmpNativeCoroutinesErrors.REDUNDANT_PRIVATE_COROUTINES_STATE
import com.rickclephas.kmp.nativecoroutines.compiler.utils.NativeCoroutinesAnnotations
import com.rickclephas.kmp.nativecoroutines.compiler.utils.CoroutinesReturnType
import com.rickclephas.kmp.nativecoroutines.compiler.utils.coroutinesReturnType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.getSourceFromAnnotation
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

internal object KmpNativeCoroutinesChecker: DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is CallableDescriptor) return
        if (descriptor !is SimpleFunctionDescriptor && descriptor !is PropertyDescriptor) return
        val annotations = NativeCoroutinesAnnotations(descriptor)
        val isPublic = descriptor.isEffectivelyPublicApi
        val isOverride = descriptor.overriddenDescriptors.isNotEmpty()
        val isSuspend = descriptor is FunctionDescriptor && descriptor.isSuspend
        val returnType = descriptor.coroutinesReturnType

        if (annotations.nativeCoroutines != null && annotations.nativeCoroutinesState != null) {
            context.trace.report(CONFLICT_COROUTINES_STATE, annotations.nativeCoroutinesState, declaration)
        }
        if (isPublic && !isOverride && annotations.nativeCoroutines == null &&
            annotations.nativeCoroutinesIgnore == null && annotations.nativeCoroutinesState == null
        ) {
            if (isSuspend) context.trace.report(EXPOSED_SUSPEND_FUNCTION.on(declaration))
            if (returnType is CoroutinesReturnType.Flow) context.trace.report(EXPOSED_FLOW_TYPE.on(declaration))
        }
        if (annotations.nativeCoroutinesIgnore != null) {
            context.trace.report(IGNORED_COROUTINES, annotations.nativeCoroutines, declaration)
            context.trace.report(IGNORED_COROUTINES_STATE, annotations.nativeCoroutinesState, declaration)
        }
        if (returnType != CoroutinesReturnType.CoroutineScope) {
            context.trace.report(INVALID_COROUTINE_SCOPE, annotations.nativeCoroutineScope, declaration)
        }
        if (!isSuspend && returnType !is CoroutinesReturnType.Flow) {
            context.trace.report(INVALID_COROUTINES, annotations.nativeCoroutines, declaration)
            context.trace.report(INVALID_COROUTINES_IGNORE, annotations.nativeCoroutinesIgnore, declaration)
        }
        if (descriptor !is PropertyDescriptor || returnType !is CoroutinesReturnType.Flow.State) {
            context.trace.report(INVALID_COROUTINES_STATE, annotations.nativeCoroutinesState, declaration)
        }
        if (isOverride) {
            context.trace.report(REDUNDANT_OVERRIDE_COROUTINES, annotations.nativeCoroutines, declaration)
            context.trace.report(REDUNDANT_OVERRIDE_COROUTINES_IGNORE, annotations.nativeCoroutinesIgnore, declaration)
            context.trace.report(REDUNDANT_OVERRIDE_COROUTINES_STATE, annotations.nativeCoroutinesState, declaration)
        }
        if (!isPublic) {
            context.trace.report(REDUNDANT_PRIVATE_COROUTINES, annotations.nativeCoroutines, declaration)
            context.trace.report(REDUNDANT_PRIVATE_COROUTINES_IGNORE, annotations.nativeCoroutinesIgnore, declaration)
            context.trace.report(REDUNDANT_PRIVATE_COROUTINES_STATE, annotations.nativeCoroutinesState, declaration)
        }
    }

    private fun BindingTrace.report(
        diagnosticFactory: DiagnosticFactory0<KtElement>,
        annotationDescriptor: AnnotationDescriptor?,
        declaration: KtDeclaration
    ) {
        if (annotationDescriptor == null) return
        report(diagnosticFactory.on(getSourceFromAnnotation(annotationDescriptor) ?: declaration))
    }
}
