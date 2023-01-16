package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun FirElement.isValidLhsOfAssignment(context: CheckerContext): Boolean {
    if (this !is FirQualifiedAccessExpression) return false
    val lastQualified = context.qualifiedAccessOrAnnotationCalls.lastOrNull { it != this } ?: return false
    return lastQualified is FirVariableAssignment && lastQualified.lValue == this
}
