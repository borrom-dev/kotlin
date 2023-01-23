/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiNameValuePair
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtElement

internal class SymbolAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
    private val arguments: List<KtNamedAnnotationValue>,
) : KtLightElementBase(parent), PsiAnnotationParameterList {

    private val _attributes: Array<PsiNameValuePair> by lazyPub {
        arguments.map {
            SymbolNameValuePairForAnnotationArgument(it, this)
        }.toTypedArray()
    }

    override fun getAttributes(): Array<PsiNameValuePair> = _attributes

    override val kotlinOrigin: KtElement? get() = null

    //TODO: EQ GHC EQIV
}
