/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.SmartList
import java.util.concurrent.atomic.AtomicReference

internal class LazyAnnotationsBox(
    private val annotationsProvider: AnnotationsProvider,
    private val additionalAnnotationsProvider: AdditionalAnnotationsProvider = EmptyAdditionalAnnotationsProvider,
) : AnnotationsBox {
    private val annotationsArray: AtomicReference<Array<PsiAnnotation>?> = AtomicReference()
    private var specialAnnotations: SmartList<PsiAnnotation>? = null
    private val monitor = Any()

    override fun annotations(owner: PsiModifierList): Array<PsiAnnotation> {
        annotationsArray.get()?.let { return it }

        val classIds = annotationsProvider.classIds()
        val annotations = classIds.withIndex().mapTo(SmartList<PsiAnnotation>()) { (index, classId) ->
            SymbolLightLazyAnnotation(classId, annotationsProvider, index, owner)
        }

        val valueToReturn = synchronized(monitor) {
            specialAnnotations?.forEach { specialAnnotation ->
                val index = annotations.indexOfFirst { it.qualifiedName == specialAnnotation.qualifiedName }
                if (index != -1) {
                    annotations[index] = specialAnnotation
                } else {
                    annotations += specialAnnotation
                }
            }

            val foundQualifiers = annotations.mapNotNullTo(hashSetOf()) { it.qualifiedName }
            additionalAnnotationsProvider.addAllAnnotations(annotations, foundQualifiers, owner)

            specialAnnotations = null
            setAnnotationsArray(if (annotations.isNotEmpty()) annotations.toTypedArray() else PsiAnnotation.EMPTY_ARRAY)
        }

        return valueToReturn
    }

    private fun setAnnotationsArray(array: Array<PsiAnnotation>): Array<PsiAnnotation> =
        if (annotationsArray.compareAndSet(null, array)) {
            array
        } else {
            annotationsArray.get() ?: error("Unexpected state")
        }

    override fun findAnnotation(
        owner: PsiModifierList,
        qualifiedName: String,
    ): PsiAnnotation? = findAnnotation(owner, qualifiedName, withAdditionalAnnotations = true)

    fun findAnnotation(owner: PsiModifierList, qualifiedName: String, withAdditionalAnnotations: Boolean): PsiAnnotation? {
        annotationsArray.get()?.let { array ->
            return array.find { it.qualifiedName == qualifiedName }
        }

        val specialAnnotationClassId = specialAnnotationsList[qualifiedName]
        val specialAnnotation = if (specialAnnotationClassId != null) {
            val annotationApplication = annotationsProvider[specialAnnotationClassId].firstOrNull() ?: return null

            SymbolLightLazyAnnotation(
                specialAnnotationClassId,
                annotationsProvider,
                annotationApplication,
                owner,
            )
        } else if (withAdditionalAnnotations) {
            additionalAnnotationsProvider.findAdditionalAnnotation(this, qualifiedName, owner)
        } else {
            null
        }

        if (specialAnnotation == null) {
            return annotations(owner).find { it.qualifiedName == qualifiedName }
        }

        return synchronized(monitor) {
            annotationsArray.get()?.let { array ->
                return array.find { it.qualifiedName == qualifiedName }
            }

            if (specialAnnotations != null) {
                val specialAnnotations = specialAnnotations!!
                val oldAnnotation = specialAnnotations.find { it.qualifiedName == specialAnnotation.qualifiedName }
                if (oldAnnotation != null) {
                    oldAnnotation
                } else {
                    specialAnnotations += specialAnnotation
                    specialAnnotation
                }
            } else {
                specialAnnotations = SmartList(specialAnnotation)
                specialAnnotation
            }
        }
    }

    override fun hasAnnotation(owner: PsiModifierList, qualifiedName: String): Boolean {
        annotationsArray.get()?.let { array ->
            return array.any { it.qualifiedName == qualifiedName }
        }

        val specialAnnotationClassId = specialAnnotationsList[qualifiedName]
        return if (specialAnnotationClassId != null) {
            specialAnnotationClassId in annotationsProvider
        } else {
            annotations(owner).any { it.qualifiedName == qualifiedName }
        }
    }

    companion object {
        /**
         * @see org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper
         */
        private val specialAnnotationsList: Map<String, ClassId> = listOf(
            StandardClassIds.Annotations.Deprecated,
//        Annotations.Retention,
//        Annotations.Java.Retention,
//        Annotations.Target,
//        Annotations.Java.Target,
//        Annotations.Java.Override,
            StandardClassIds.Annotations.DeprecatedSinceKotlin,
            StandardClassIds.Annotations.WasExperimental,
            StandardClassIds.Annotations.JvmRecord,
//        Annotations.Repeatable,
//        Annotations.Java.Repeatable,
        ).associateBy { it.asFqNameString() }
    }
}
