/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.synthetic

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyAccessorImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class FirSyntheticPropertyAccessor(
    val delegate: FirSimpleFunction,
    override val isGetter: Boolean,
    override val propertySymbol: FirPropertySymbol,
) : FirPropertyAccessor() {
    override val source: KtSourceElement?
        get() = delegate.source

    override val moduleData: FirModuleData
        get() = delegate.moduleData

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Synthetic

    override val returnTypeRef: FirTypeRef
        get() = delegate.returnTypeRef

    override val resolvePhase: FirResolvePhase
        get() = delegate.resolvePhase

    override val status: FirDeclarationStatus
        get() = delegate.status

    override val dispatchReceiverType: ConeSimpleKotlinType?
        get() = delegate.dispatchReceiverType

    override val receiverParameter: FirReceiverParameter?
        get() = null
    
    override val deprecationsProvider: DeprecationsProvider
        get() = delegate.deprecationsProvider

    override val valueParameters: List<FirValueParameter>
        get() = delegate.valueParameters

    override val annotations: List<FirAnnotation>
        get() = delegate.annotations

    override val typeParameters: List<FirTypeParameter>
        get() = emptyList()

    override val isSetter: Boolean
        get() = !isGetter

    override val body: FirBlock?
        get() = delegate.body

    override val attributes: FirDeclarationAttributes
        get() = delegate.attributes

    override val symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol().apply {
        bind(this@FirSyntheticPropertyAccessor)
    }

    override val contextReceivers: List<FirContextReceiver>
        get() = emptyList()

    override val controlFlowGraphReference: FirControlFlowGraphReference? = null

    override val contractDescription: FirContractDescription = FirEmptyContractDescription

    override val containerSource: DeserializedContainerSource? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        delegate.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        contractDescription.accept(visitor, data)
    }

    override fun replaceBody(newBody: FirBlock?) {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirPropertyAccessor {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirPropertyAccessor {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirPropertyAccessor {
        throw AssertionError("Transformation of synthetic property accessor isn't supported")
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }

    override fun replaceContractDescription(newContractDescription: FirContractDescription) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }

    override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        throw AssertionError("Mutation of synthetic property accessor isn't supported")
    }
}
