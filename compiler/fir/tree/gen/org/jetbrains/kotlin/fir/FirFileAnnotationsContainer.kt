/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirFileAnnotationsContainer : FirElementWithResolvePhase(), FirAnnotationContainer {
    abstract override val source: KtSourceElement?
    abstract override val resolvePhase: FirResolvePhase
    abstract override val moduleData: FirModuleData
    abstract override val annotations: List<FirAnnotation>
    abstract val containingFileSymbol: FirFileSymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitFileAnnotationsContainer(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformFileAnnotationsContainer(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFileAnnotationsContainer
}
