/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethod
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import java.lang.annotation.ElementType

internal object DefaultAnnotationsProvider : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiModifierList,
    ) {
        val parent = owner.parent
        if (parent.isAnnotationClass()) {
            addAllAnnotationsFromAnnotationClass(currentRawAnnotations, foundQualifiers, owner)
        } else if (parent.isMethodWithOverride()) {
            addSimpleAnnotationIfMissing(JvmAnnotationNames.OVERRIDE_ANNOTATION.asString(), currentRawAnnotations, foundQualifiers, owner)
        }
    }

    override fun findAdditionalAnnotation(
        annotationsBox: LazyAnnotationsBox,
        qualifiedName: String,
        owner: PsiModifierList,
    ): PsiAnnotation? {
        val parent = owner.parent
        return when {
            parent.isAnnotationClass() -> findAdditionalAnnotationFromAnnotationClass(annotationsBox, qualifiedName, owner)
            parent.isMethodWithOverride() -> {
                val overrideQualifier = JvmAnnotationNames.OVERRIDE_ANNOTATION.asString()
                if (qualifiedName == overrideQualifier) SymbolLightSimpleAnnotation(overrideQualifier, owner) else null
            }

            else -> null
        }
    }
}

private fun addAllAnnotationsFromAnnotationClass(
    currentRawAnnotations: MutableList<in PsiAnnotation>,
    foundQualifiers: MutableSet<String>,
    owner: PsiModifierList,
) {
    for (index in currentRawAnnotations.indices) {
        val currentAnnotation = currentRawAnnotations[index] as? SymbolLightLazyAnnotation ?: continue
        val newAnnotation = currentAnnotation.tryConvertToRetentionJavaAnnotation(owner)
            ?: currentAnnotation.tryConvertToTargetJavaAnnotation(owner)
            ?: currentAnnotation.tryConvertToDocumentedJavaAnnotation(owner)
            ?: currentAnnotation.tryConvertToRepeatableJavaAnnotation(owner)
            ?: continue

        val qualifiedName = newAnnotation.qualifiedName
        requireNotNull(qualifiedName) { "The annotation must have 'qualifiedName'" }

        if (!foundQualifiers.add(qualifiedName)) continue
        currentRawAnnotations += newAnnotation
    }

    if (foundQualifiers.add(JvmAnnotationNames.RETENTION_ANNOTATION.asString())) {
        currentRawAnnotations += createRetentionJavaAnnotation(owner)
    }
}

private fun findAdditionalAnnotationFromAnnotationClass(
    annotationsBox: LazyAnnotationsBox,
    qualifiedName: String,
    owner: PsiModifierList,
): PsiAnnotation? = annotationsBox.tryConvertToRetentionJavaAnnotation(qualifiedName, owner)
    ?: annotationsBox.tryConvertToTargetJavaAnnotation(qualifiedName, owner)
    ?: annotationsBox.tryConvertToDocumentedJavaAnnotation(qualifiedName, owner)
    ?: annotationsBox.tryConvertToRepeatableJavaAnnotation(qualifiedName, owner)

private fun PsiElement.isAnnotationClass(): Boolean = this is PsiClass && isAnnotationType
private fun PsiElement.isMethodWithOverride(): Boolean = this is SymbolLightMethod<*> && (isDelegated || isOverride())

private fun addSimpleAnnotationIfMissing(
    qualifier: String,
    currentRawAnnotations: MutableList<in PsiAnnotation>,
    foundQualifiers: MutableSet<String>,
    owner: PsiModifierList,
) {
    if (!foundQualifiers.add(qualifier)) return
    currentRawAnnotations += SymbolLightSimpleAnnotation(qualifier, owner)
}

private fun LazyAnnotationsBox.tryConvertToDocumentedJavaAnnotation(
    qualifiedName: String,
    owner: PsiModifierList,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    qualifiedName = qualifiedName,
    javaQualifier = JvmAnnotationNames.DOCUMENTED_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.mustBeDocumented.asString(),
    owner = owner,
)

private fun SymbolLightLazyAnnotation.tryConvertToDocumentedJavaAnnotation(
    owner: PsiModifierList,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    javaQualifier = JvmAnnotationNames.DOCUMENTED_ANNOTATION.asString(),
    owner = owner,
)

private fun LazyAnnotationsBox.tryConvertToRetentionJavaAnnotation(
    qualifiedName: String,
    owner: PsiModifierList,
): PsiAnnotation? {
    val javaQualifier = JvmAnnotationNames.RETENTION_ANNOTATION.asString()
    return tryConvertToJavaAnnotation(
        qualifiedName = qualifiedName,
        javaQualifier = javaQualifier,
        kotlinQualifier = StandardNames.FqNames.retention.asString(),
        owner = owner,
        argumentsComputer = SymbolLightJavaAnnotation::computeJavaRetentionArguments,
    ) ?: owner.takeIf { qualifiedName == javaQualifier }?.let(::createRetentionJavaAnnotation)
}

private fun SymbolLightLazyAnnotation.tryConvertToRetentionJavaAnnotation(
    owner: PsiModifierList,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    javaQualifier = JvmAnnotationNames.RETENTION_ANNOTATION.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeJavaRetentionArguments,
)

private fun SymbolLightJavaAnnotation.computeJavaRetentionArguments(): List<KtNamedAnnotationValue> {
    val argumentWithKotlinRetention = originalLightAnnotation.annotationApplication
        .value
        .arguments
        .firstOrNull {
            it.name == StandardNames.DEFAULT_VALUE_PARAMETER
        }?.expression as? KtEnumEntryAnnotationValue

    val kotlinRetentionName = argumentWithKotlinRetention?.callableId?.callableName?.asString()
    return javaRetentionArguments(kotlinRetentionName)
}

private fun createRetentionJavaAnnotation(owner: PsiModifierList): PsiAnnotation = SymbolLightSimpleAnnotation(
    fqName = JvmAnnotationNames.RETENTION_ANNOTATION.asString(),
    parent = owner,
    arguments = javaRetentionArguments(kotlinRetentionName = null),
)

private fun javaRetentionArguments(kotlinRetentionName: String?): List<KtNamedAnnotationValue> = listOf(
    KtNamedAnnotationValue(
        name = StandardNames.DEFAULT_VALUE_PARAMETER,
        expression = KtEnumEntryAnnotationValue(
            callableId = CallableId(
                StandardClassIds.Annotations.Java.RetentionPolicy,
                Name.identifier(kotlinRetentionName ?: AnnotationRetention.RUNTIME.name),
            ),
            sourcePsi = null,
        )
    )
)

private fun LazyAnnotationsBox.tryConvertToRepeatableJavaAnnotation(
    qualifiedName: String,
    owner: PsiModifierList,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    qualifiedName = qualifiedName,
    javaQualifier = JvmAnnotationNames.REPEATABLE_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.repeatable.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeRepeatableJavaAnnotationArguments
)

private fun SymbolLightLazyAnnotation.tryConvertToRepeatableJavaAnnotation(
    owner: PsiModifierList,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    javaQualifier = JvmAnnotationNames.REPEATABLE_ANNOTATION.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeRepeatableJavaAnnotationArguments,
)

private fun SymbolLightJavaAnnotation.computeRepeatableJavaAnnotationArguments(): List<KtNamedAnnotationValue> {
    val annotationClassId = originalLightAnnotation.withAnnotatedSymbol { ktAnnotatedSymbol ->
        (ktAnnotatedSymbol as? KtClassLikeSymbol)?.classIdIfNonLocal
    } ?: return emptyList()

    return listOf(
        KtNamedAnnotationValue(
            name = StandardNames.DEFAULT_VALUE_PARAMETER,
            expression = KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue(
                classId = annotationClassId.createNestedClassId(Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME)),
                sourcePsi = null,
            )
        )
    )
}

private fun LazyAnnotationsBox.tryConvertToTargetJavaAnnotation(
    qualifiedName: String,
    owner: PsiModifierList,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    qualifiedName = qualifiedName,
    javaQualifier = JvmAnnotationNames.TARGET_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.target.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeTargetJavaAnnotationArguments,
)

private fun SymbolLightLazyAnnotation.tryConvertToTargetJavaAnnotation(
    owner: PsiModifierList,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    javaQualifier = JvmAnnotationNames.TARGET_ANNOTATION.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeTargetJavaAnnotationArguments,
)

private fun SymbolLightJavaAnnotation.computeTargetJavaAnnotationArguments(): List<KtNamedAnnotationValue> {
    val allowedKotlinTargets = originalLightAnnotation.annotationApplication
        .value
        .arguments
        .firstOrNull()
        ?.expression as? KtArrayAnnotationValue
        ?: return emptyList()

    val javaTargetNames = allowedKotlinTargets.values.mapNotNullTo(linkedSetOf(), KtAnnotationValue::mapToJavaTarget)
    return listOf(
        KtNamedAnnotationValue(
            name = StandardNames.DEFAULT_VALUE_PARAMETER,
            expression = KtArrayAnnotationValue(
                values = javaTargetNames.map {
                    KtEnumEntryAnnotationValue(
                        callableId = CallableId(
                            classId = StandardClassIds.Annotations.Java.ElementType,
                            callableName = Name.identifier(it),
                        ),
                        sourcePsi = null,
                    )
                },
                sourcePsi = null,
            )
        )
    )
}

private fun KtAnnotationValue.mapToJavaTarget(): String? {
    if (this !is KtEnumEntryAnnotationValue) return null

    val callableId = callableId ?: return null
    if (callableId.classId != StandardClassIds.AnnotationTarget) return null
    return when (callableId.callableName.asString()) {
        AnnotationTarget.CLASS.name -> ElementType.TYPE
        AnnotationTarget.ANNOTATION_CLASS.name -> ElementType.ANNOTATION_TYPE
        AnnotationTarget.FIELD.name -> ElementType.FIELD
        AnnotationTarget.LOCAL_VARIABLE.name -> ElementType.LOCAL_VARIABLE
        AnnotationTarget.VALUE_PARAMETER.name -> ElementType.PARAMETER
        AnnotationTarget.CONSTRUCTOR.name -> ElementType.CONSTRUCTOR
        AnnotationTarget.FUNCTION.name, AnnotationTarget.PROPERTY_GETTER.name, AnnotationTarget.PROPERTY_SETTER.name -> ElementType.METHOD
        AnnotationTarget.TYPE_PARAMETER.name -> ElementType.TYPE_PARAMETER
        AnnotationTarget.TYPE.name -> ElementType.TYPE_USE
        else -> null
    }?.name
}

private fun LazyAnnotationsBox.tryConvertToJavaAnnotation(
    qualifiedName: String,
    javaQualifier: String,
    kotlinQualifier: String,
    owner: PsiModifierList,
    argumentsComputer: SymbolLightJavaAnnotation.() -> List<KtNamedAnnotationValue> = { emptyList() },
): PsiAnnotation? {
    if (qualifiedName != javaQualifier) return null
    if (hasAnnotation(javaQualifier)) return null

    val originalLightAnnotation = findAnnotation(
        kotlinQualifier,
        withAdditionalAnnotations = false,
    ) as? SymbolLightLazyAnnotation ?: return null

    return SymbolLightJavaAnnotation(
        originalLightAnnotation = originalLightAnnotation,
        javaQualifier = javaQualifier,
        argumentsComputer = argumentsComputer,
        owner = owner,
    )
}

private fun SymbolLightLazyAnnotation.tryConvertToJavaAnnotation(
    javaQualifier: String,
    owner: PsiModifierList,
    argumentsComputer: SymbolLightJavaAnnotation.() -> List<KtNamedAnnotationValue> = { emptyList() },
): PsiAnnotation? {
    if (qualifiedName != javaQualifier) return null
    return SymbolLightJavaAnnotation(
        originalLightAnnotation = this,
        javaQualifier = javaQualifier,
        argumentsComputer = argumentsComputer,
        owner = owner,
    )
}
