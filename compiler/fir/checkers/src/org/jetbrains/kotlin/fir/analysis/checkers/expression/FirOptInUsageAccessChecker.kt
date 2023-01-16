/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.isValidLhsOfAssignment
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.coneType

object FirOptInUsageAccessChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        val sourceKind = expression.source?.kind
        if (sourceKind is KtFakeSourceElementKind.DataClassGeneratedMembers ||
            sourceKind is KtFakeSourceElementKind.PropertyFromParameter
        ) return

        if (expression.isValidLhsOfAssignment(context)) return

        val resolvedSymbol = expression.calleeReference?.toResolvedBaseSymbol() ?: return

        with(FirOptInUsageBaseChecker) {
            if (expression is FirVariableAssignment) {
                val experimentalities = resolvedSymbol.loadExperimentalities(context, fromSetter = true, null) +
                        loadExperimentalitiesFromTypeArguments(context, emptyList())
                reportNotAcceptedExperimentalities(experimentalities, expression.lValue, context, reporter)
            } else if (expression is FirQualifiedAccessExpression) {
                val dispatchReceiverType =
                    expression.dispatchReceiver.takeIf { it !is FirNoReceiverExpression }?.typeRef?.coneType?.fullyExpandedType(context.session)

                val experimentalities = resolvedSymbol.loadExperimentalities(context, fromSetter = false, dispatchReceiverType) +
                        loadExperimentalitiesFromTypeArguments(context, expression.typeArguments)
                reportNotAcceptedExperimentalities(experimentalities, expression, context, reporter)
            }
        }
    }
}
