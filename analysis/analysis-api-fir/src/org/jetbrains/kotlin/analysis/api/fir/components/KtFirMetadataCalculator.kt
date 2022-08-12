/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.analysis.api.components.KtMetadataCalculator
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmSerializerExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.load.kotlin.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirMetadataCalculator(
    override val analysisSession: KtFirAnalysisSession
) : KtMetadataCalculator(), KtFirAnalysisSessionComponent {
    private val firSession: FirSession
        get() = firResolveSession.useSiteFirSession

    private val scopeSession: ScopeSession
        get() = firResolveSession.getScopeSessionFor(firSession)

    override fun calculate(ktClass: KtClassOrObject): Metadata {
        // TODO: support nested classes
//        val firClasses = ktClass.parentsWithSelf
//            .filterIsInstance<KtClassOrObject>()
//            .map { it.getOrBuildFirOfType<FirRegularClass>(firResolveSession) }
//            .toList()
        val firClass = ktClass.getOrBuildFirOfType<FirRegularClass>(firResolveSession)
        firClass.symbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        val (serializer, stringTable) = createTopLevelSerializer(FirMetadataSource.Class(firClass))
        val classProto = serializer.classProto(firClass)
        return generateAnnotation(classProto.build(), stringTable, Kind.Class)
    }

    override fun calculate(ktFile: KtFile): Metadata {
        val firFile = ktFile.getOrBuildFirFile(firResolveSession)
        firFile.symbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        val (serializer, stringTable) = createTopLevelSerializer(FirMetadataSource.File(firFile))
        val fileProto = serializer.packagePartProto(firFile.packageFqName, firFile)

        return generateAnnotation(fileProto.build(), stringTable, Kind.File)
    }

    override fun calculate(ktFiles: Collection<KtFile>): Metadata? {
        TODO("support multifile facades")
    }

    private enum class Kind(val value: Int) {
        Class(1),
        File(2),
        SyntheticClass(3),
        MultifileClassFacade(4),
        MultifileClassPart(5)
    }

    private fun generateAnnotation(message: GeneratedMessageLite, stringTable: JvmStringTable, kind: Kind): Metadata {
        return Metadata(
            kind = kind.value,
            metadataVersion = JvmBytecodeBinaryVersion.INSTANCE.toArray(),
            bytecodeVersion = intArrayOf(1, 0, 3),
            data1 = JvmProtoBufUtil.writeData(message, stringTable),
            data2 = ArrayUtil.toStringArray(stringTable.strings),
            extraString = "",
            packageName = "",
            extraInt = 0
        )
    }

    private fun createTopLevelSerializer(metadata: FirMetadataSource): Pair<FirElementSerializer, JvmStringTable> {
        val session = firSession
        val scopeSession = scopeSession
        val typeApproximator = session.typeApproximator
        val stringTable = FirJvmElementAwareStringTableForLightClasses()
        val jvmSerializerExtension = FirJvmSerializerExtension(
            session,
            JvmSerializationBindings(),
            metadata,
            localDelegatedProperties = emptyList(),
            typeApproximator,
            scopeSession,
            JvmSerializationBindings(),
            useTypeTable = true,
            moduleName = analysisSession.useSiteModule.moduleDescription,
            classBuilderMode = ClassBuilderMode.KAPT3,
            isParamAssertionsDisabled = true,
            unifiedNullChecks = false,
            metadataVersion = GenerationState.LANGUAGE_TO_METADATA_VERSION.getValue(session.languageVersionSettings.languageVersion),
            jvmDefaultMode = JvmDefaultMode.ENABLE,
            stringTable
        )
        return FirElementSerializer.createTopLevel(
            session,
            scopeSession,
            jvmSerializerExtension,
            typeApproximator
        ) to stringTable
    }

    private class FirJvmElementAwareStringTableForLightClasses : JvmStringTable(), FirElementAwareStringTable {
        override fun getLocalClassIdReplacement(firClass: FirClass): ClassId {
            error("Should not be called")
        }
    }
}

