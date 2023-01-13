/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType

public abstract class KtSubtypingComponent : KtAnalysisSessionComponent() {
    public abstract fun isEqualTo(first: KtType, second: KtType): Boolean
    public abstract fun isSubTypeOf(subType: KtType, superType: KtType): Boolean
    public abstract fun isPossiblySubTypeOf(subType: KtType, superType: KtType, freeTypeParameters: List<KtTypeParameterSymbol>): Boolean
}

public interface KtSubtypingComponentMixIn : KtAnalysisSessionMixIn {
    infix public fun KtType.isEqualTo(other: KtType): Boolean =
        withValidityAssertion { analysisSession.subtypingComponent.isEqualTo(this, other) }

    infix public fun KtType.isSubTypeOf(superType: KtType): Boolean =
        withValidityAssertion { analysisSession.subtypingComponent.isSubTypeOf(this, superType) }

    infix public fun KtType.isNotSubTypeOf(superType: KtType): Boolean =
        withValidityAssertion { !analysisSession.subtypingComponent.isSubTypeOf(this, superType) }

    /**
     * @return true, if there exists such substitution of [freeTypeParameters] that [this] is a subtype of [superType].
     * For example, `MutableList<T>` is a possible subtype of `List<String>`, given that `T` is in [freeTypeParameters].
     */
    public fun KtType.isPossiblySubTypeOf(superType: KtType, freeTypeParameters: List<KtTypeParameterSymbol>): Boolean =
        withValidityAssertion { analysisSession.subtypingComponent.isPossiblySubTypeOf(this, superType, freeTypeParameters) }
}