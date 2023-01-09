/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryProperResolverWithAttributes
import org.jetbrains.kotlin.library.UnresolvedLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolver
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.util.Logger

class JsLibraryResolver(
    repositories: List<String>,
    directLibs: List<String>,
    distributionKlib: String?,
    localKotlinDir: String?,
    skipCurrentDir: Boolean,
    logger: Logger
) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
    repositories,
    directLibs,
    distributionKlib,
    localKotlinDir,
    skipCurrentDir,
    logger,
    emptyList()
) {
    // Stick with the default KotlinLibrary for now.
    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKotlinLibraryComponents(file, isDefault)
}

// TODO: This is a temporary set of library resolver policies for js compiler.
fun jsResolveLibraries(libraries: Collection<String>, repositories: Collection<String>, logger: Logger): KotlinLibraryResolveResult =
    jsResolveLibraries(
        libraries,
        repositories,
        logger
    ) { libraryResolver ->
        libraryResolver::resolveWithDependencies
    }

fun jsResolveLibrariesWithoutDependencies(
    libraries: Collection<String>,
    repositories: Collection<String>,
    logger: Logger
): List<KotlinLibrary> =
    jsResolveLibraries(
        libraries,
        repositories,
        logger
    ) { libraryResolver ->
        libraryResolver::resolveWithoutDependencies
    }

private fun <T> jsResolveLibraries(
    libraries: Collection<String>,
    repositories: Collection<String>,
    logger: Logger,
    resolver: (KotlinLibraryResolver<KotlinLibrary>) -> ((unresolvedLibraries: List<UnresolvedLibrary>, noStdLib: Boolean, noDefaultLibs: Boolean, noEndorsedLibs: Boolean) -> T)
): T {
    val unresolvedLibraries = libraries.map { UnresolvedLibrary(it, null) }
    val libraryAbsolutePaths = libraries.map { File(it).absolutePath }
    // Configure the resolver to only work with absolute paths for now.
    val libraryResolver = JsLibraryResolver(
        repositories = repositories.toList(),
        directLibs = libraryAbsolutePaths,
        distributionKlib = null,
        localKotlinDir = null,
        skipCurrentDir = false,
        logger = logger
    ).libraryResolver()
    return resolver(libraryResolver)(
        /* unresolvedLibraries = */unresolvedLibraries,
        /* noStdLib = */true,
        /* noDefaultLibs = */true,
        /* noEndorsedLibs = */true
    )
}