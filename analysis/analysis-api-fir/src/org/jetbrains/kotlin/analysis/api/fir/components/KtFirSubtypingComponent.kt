/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtSubtypingComponent
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.resolve.calls.ConeSimpleConstraintSystemImpl
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.safeSubstitute

internal class KtFirSubtypingComponent(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtSubtypingComponent(), KtFirAnalysisSessionComponent {
    override fun isEqualTo(first: KtType, second: KtType): Boolean {
        second.assertIsValidAndAccessible()
        check(first is KtFirType)
        check(second is KtFirType)
        return AbstractTypeChecker.equalTypes(
            createTypeCheckerContext(),
            first.coneType,
            second.coneType
        )
    }

    override fun isSubTypeOf(subType: KtType, superType: KtType): Boolean {
        superType.assertIsValidAndAccessible()
        check(subType is KtFirType)
        check(superType is KtFirType)
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerContext(),
            subType.coneType,
            superType.coneType
        )
    }

    override fun isPossiblySubTypeOf(subType: KtType, superType: KtType, freeTypeParameters: List<KtTypeParameterSymbol>): Boolean {
        if (freeTypeParameters.isEmpty()) return isSubTypeOf(subType, superType)

        superType.assertIsValidAndAccessible()
        check(subType is KtFirType)
        check(superType is KtFirType)

        val inferenceComponents = analysisSession.firResolveSession.useSiteFirSession.inferenceComponents
        val constraintSystem = ConeSimpleConstraintSystemImpl(inferenceComponents.createConstraintSystem(), inferenceComponents.session)

        val typeSubstitutor = constraintSystem.registerTypeVariables(freeTypeParameters.map { it.firSymbol.toLookupTag() })
        val subTypeSubstituted = typeSubstitutor.safeSubstitute(constraintSystem.context, subType.coneType)
        val superTypeSubstituted = typeSubstitutor.safeSubstitute(constraintSystem.context, superType.coneType)

        constraintSystem.addSubtypeConstraint(subTypeSubstituted, superTypeSubstituted)
        return !constraintSystem.hasContradiction()
    }
}
