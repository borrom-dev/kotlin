/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationClassIds
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithArguments
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithClassIds
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForReceiverParameter private constructor(
    private val firCallableSymbol: FirCallableSymbol<*>,
    private val receiverParameter: FirAnnotationContainer,
    private val useSiteSession: FirSession,
    override val token: KtLifetimeToken,
) : KtAnnotationsList() {

    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion {
            receiverParameter.resolvedAnnotationsWithArguments(firCallableSymbol).map { annotation ->
                annotation.toKtAnnotationApplication(useSiteSession)
            }
        }

    override fun hasAnnotation(
        classId: ClassId,
        useSiteTarget: AnnotationUseSiteTarget?,
        acceptAnnotationsWithoutUseSite: Boolean,
    ): Boolean = withValidityAssertion {
        receiverParameter.resolvedAnnotationsWithClassIds(firCallableSymbol).any {
            (it.useSiteTarget == useSiteTarget || acceptAnnotationsWithoutUseSite && it.useSiteTarget == null) &&
                    it.toAnnotationClassId(useSiteSession) == classId
        }
    }

    override fun hasAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        receiverParameter.resolvedAnnotationsWithClassIds(firCallableSymbol).hasAnnotation(classId, useSiteSession)
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = withValidityAssertion {
        receiverParameter.resolvedAnnotationsWithArguments(firCallableSymbol).mapNotNull { annotation ->
            if (annotation.toAnnotationClassId(useSiteSession) != classId) return@mapNotNull null
            annotation.toKtAnnotationApplication(useSiteSession)
        }
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            receiverParameter.resolvedAnnotationClassIds(firCallableSymbol)
        }

    companion object {
        fun create(
            firCallableSymbol: FirCallableSymbol<*>,
            useSiteSession: FirSession,
            token: KtLifetimeToken,
        ): KtAnnotationsList {
            val receiverParameter = firCallableSymbol.receiverParameter
            return if (receiverParameter?.annotations?.isEmpty() != false) {
                KtEmptyAnnotationsList(token)
            } else {
                KtFirAnnotationListForReceiverParameter(firCallableSymbol, receiverParameter, useSiteSession, token)
            }
        }
    }
}
