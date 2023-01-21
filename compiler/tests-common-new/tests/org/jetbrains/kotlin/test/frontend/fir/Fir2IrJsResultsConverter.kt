/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.AbstractFirAnalyzerFacade
import org.jetbrains.kotlin.fir.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.serialization.FirElementAwareSerializableStringTable
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.incrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.jsLibraryProvider
import org.jetbrains.kotlin.utils.metadataVersion

class Fir2IrJsResultsConverter(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val isMppSupported = module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val componentsMap = mutableMapOf<String, Fir2IrComponents>()
        lateinit var mainIrPart: IrModuleFragment
        val dependentIrParts = mutableListOf<IrModuleFragment>()
        val sourceFiles = mutableListOf<KtSourceFile>()
        val firFilesAndComponentsBySourceFile = mutableMapOf<KtSourceFile, Pair<FirFile, Fir2IrComponents>>()
        lateinit var mainPluginContext: IrPluginContext
        var currentSymbolTable: SymbolTable? = null

        for ((index, part) in inputArtifact.partsForDependsOnModules.withIndex()) {
            val dependentComponents = mutableListOf<Fir2IrComponents>()
            if (isMppSupported) {
                for (dependency in part.module.dependsOnDependencies) {
                    dependentComponents.add(componentsMap[dependency.moduleName]!!)
                }
            }

            val (irModuleFragment, components, pluginContext) =
                part.firAnalyzerFacade.convertToJsIr(
                    part.firFiles.values,
                    fir2IrExtensions = Fir2IrExtensions.Default,
                    module,
                    configuration,
                    testServices,
                    dependentComponents,
                    currentSymbolTable
                )
            componentsMap[part.module.name] = components
            currentSymbolTable = components.symbolTable
            mainPluginContext = pluginContext

            if (index < inputArtifact.partsForDependsOnModules.size - 1) {
                dependentIrParts.add(irModuleFragment)
            } else {
                mainIrPart = irModuleFragment
            }

            sourceFiles.addAll(part.firFiles.mapNotNull { it.value.sourceFile })

            for (firFile in part.firFiles.values) {
                firFile.sourceFile?.let {
                    firFilesAndComponentsBySourceFile[it] = firFile to components
                }
            }
        }

        val metadataVersion = configuration.metadataVersion(module.languageVersionSettings.languageVersion)

        // At this point, checkers will already have been run by a previous test step. `runCheckers` returns the cached diagnostics map.
        val diagnosticsMap = inputArtifact.partsForDependsOnModules.fold(mutableMapOf<FirFile, List<KtDiagnostic>>()) { result, part ->
            result.also { it.putAll(part.firAnalyzerFacade.runCheckers()) }
        }
        val hasErrors = diagnosticsMap.any { entry -> entry.value.any { it.severity == Severity.ERROR } }

        return IrBackendInput.JsIrBackendInput(
            mainIrPart,
            dependentIrParts,
            mainPluginContext,
            sourceFiles,
            configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList(),
            expectDescriptorToSymbol = mutableMapOf(),
            hasErrors = hasErrors
        ) { file ->
            val (firFile, components) = firFilesAndComponentsBySourceFile[file]
                ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
            serializeSingleFirFile(
                firFile,
                components.session,
                components.scopeSession,
                FirKLibSerializerExtension(components.session, metadataVersion, FirElementAwareSerializableStringTable()),
                configuration.languageVersionSettings,
            )
        }
    }
}

fun AbstractFirAnalyzerFacade.convertToJsIr(
    firFiles: Collection<FirFile>,
    fir2IrExtensions: Fir2IrExtensions,
    module: TestModule,
    configuration: CompilerConfiguration,
    testServices: TestServices,
    dependentComponents: List<Fir2IrComponents>,
    symbolTable: SymbolTable?
): Fir2IrResult {
    this as FirAnalyzerFacade
    val signaturer = IdSignatureDescriptor(JsManglerDesc)

    // TODO: consider avoiding repeated libraries resolution
    val libraries = resolveJsLibraries(module, testServices, configuration)
    val (dependencies, builtIns) = loadResolvedLibraries(libraries, configuration.languageVersionSettings, testServices)

    return Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
        session, scopeSession, firFiles.toList(),
        languageVersionSettings, signaturer,
        fir2IrExtensions,
        FirJvmKotlinMangler(), // TODO: replace with potentially simpler JS version
        JsManglerIr, IrFactoryImpl,
        Fir2IrVisibilityConverter.Default,
        Fir2IrJvmSpecialAnnotationSymbolProvider(), // TODO: replace with appropriate (probably empty) implementation
        irGeneratorExtensions,
        generateSignatures = false,
        kotlinBuiltIns = builtIns ?: DefaultBuiltIns.Instance, // TODO: consider passing externally,
        dependentComponents = dependentComponents,
        currentSymbolTable = symbolTable
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = dependencies }
    }
}

private fun loadResolvedLibraries(
    resolvedLibraries: List<KotlinResolvedLibrary>,
    languageVersionSettings: LanguageVersionSettings,
    testServices: TestServices
): Pair<List<ModuleDescriptor>, KotlinBuiltIns?> {
    var builtInsModule: KotlinBuiltIns? = null
    val dependencies = mutableListOf<ModuleDescriptorImpl>()

    return resolvedLibraries.map { resolvedLibrary ->
        // resolvedLibrary.library.libraryName in fact resolves to (modified) file path, which is confising and maybe should be refactored
        testServices.jsLibraryProvider.getOrCreateStdlibByPath(resolvedLibrary.library.libraryName) {
            // TODO: check safety of the approach of creating a separate storage manager per library
            val storageManager = LockBasedStorageManager("ModulesStructure")

            val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                resolvedLibrary.library,
                languageVersionSettings,
                storageManager,
                builtInsModule,
                packageAccessHandler = null,
                lookupTracker = LookupTracker.DO_NOTHING
            )
            dependencies += moduleDescriptor
            moduleDescriptor.setDependencies(ArrayList(dependencies))

            Pair(moduleDescriptor, resolvedLibrary.library)
        }.also {
            val isBuiltIns = resolvedLibrary.library.unresolvedDependencies.isEmpty()
            if (isBuiltIns) builtInsModule = it.builtIns
        }
    } to builtInsModule
}
