package com.rickclephas.kmp.nativecoroutines.compiler.fir.codegen

import com.rickclephas.kmp.nativecoroutines.compiler.fir.utils.*
import com.rickclephas.kmp.nativecoroutines.compiler.utils.CallableSignature
import com.rickclephas.kmp.nativecoroutines.compiler.utils.ClassIds
import com.rickclephas.kmp.nativecoroutines.compiler.utils.NativeCoroutinesAnnotation
import com.rickclephas.kmp.nativecoroutines.compiler.utils.shouldRefineInSwift
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirPropertyBodyResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.StandardClassIds

internal fun FirExtension.buildSharedFlowReplayCacheProperty(
    callableId: CallableId,
    originalSymbol: FirPropertySymbol,
    annotation: NativeCoroutinesAnnotation,
    objCNameSuffix: String?
): FirPropertySymbol? {
    val firCallableSignature = originalSymbol.getCallableSignature(session) ?: return null
    val callableSignature = firCallableSignature.signature
    if (callableSignature.isSuspend) return null
    if (callableSignature.returnType !is CallableSignature.Type.Flow.Shared) return null
    return buildProperty {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = NativeCoroutinesDeclarationKey(
            NativeCoroutinesDeclarationKey.Type.SHARED_FLOW_REPLAY_CACHE,
            callableSignature
        ).origin

        source = originalSymbol.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)

        symbol = FirPropertySymbol(callableId)
        name = callableId.callableName

        status = originalSymbol.getGeneratedDeclarationStatus(session) ?: return null

        dispatchReceiverType = originalSymbol.dispatchReceiverType

        val typeParameters = buildTypeParametersCopy(
            originalSymbol.typeParameterSymbols,
            symbol,
            origin
        )
        this.typeParameters.addAll(typeParameters.parameters)

        // TODO: support contextReceivers once exported to ObjC

        receiverParameter = buildReceiverParameterCopy(
            originalSymbol.receiverParameterSymbol,
            symbol,
            origin,
            typeParameters.substitutor
        )

        returnTypeRef = StandardClassIds.List.constructClassLikeType(
            arrayOf(firCallableSignature.getNativeType(callableSignature.returnType.valueType)),
            callableSignature.returnType.isNullable
        ).let(typeParameters.substitutor::substituteOrSelf).toFirResolvedTypeRef()

        isVar = false
        getter = buildPropertyGetter(this, originalSymbol)

        isLocal = false
        bodyResolveState = FirPropertyBodyResolveState.ALL_BODIES_RESOLVED

        @OptIn(SymbolInternals::class)
        deprecationsProvider = originalSymbol.fir.deprecationsProvider

        annotations.addAll(buildAnnotationsCopy(originalSymbol.resolvedAnnotationsWithClassIds, objCNameSuffix = objCNameSuffix))
        if (annotation.shouldRefineInSwift) {
            annotations.add(buildAnnotation(ClassIds.shouldRefineInSwift))
        }
    }.symbol
}
