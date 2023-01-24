import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import java.util.*

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(commonDependency("junit:junit"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(intellijCore())
}

val generationRoot = projectDir.resolve("tests-gen")

optInToExperimentalCompilerApi()

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

val d8Plugin = D8RootPlugin.apply(rootProject)
d8Plugin.version = v8Version

fun Test.setupV8() {
    dependsOn(d8Plugin.setupTaskProvider)
    val v8ExecutablePath = project.provider {
        d8Plugin.requireConfigured().executablePath.absolutePath
    }
    doFirst {
        systemProperty("javascript.engine.path.V8", v8ExecutablePath.get())
    }
}

fun Test.setupWasmStdlib() {
    dependsOn(":kotlin-stdlib-wasm:compileKotlinWasm")
    systemProperty("kotlin.wasm.stdlib.path", "libraries/stdlib/wasm/build/classes/kotlin/wasm/main")
    dependsOn(":kotlin-test:kotlin-test-wasm:compileKotlinWasm")
    systemProperty("kotlin.wasm.kotlin.test.path", "libraries/kotlin.test/wasm/build/classes/kotlin/wasm/main")
}

fun Test.setupGradlePropertiesForwarding() {
    val rootLocalProperties = Properties().apply {
        rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use {
            load(it)
        }
    }

    val allProperties = properties + rootLocalProperties

    val prefixForPropertiesToForward = "fd."
    for ((key, value) in allProperties) {
        if (key is String && key.startsWith(prefixForPropertiesToForward)) {
            systemProperty(key.substring(prefixForPropertiesToForward.length), value!!)
        }
    }
}

testsJar {}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateWasmTestsKt") {
    dependsOn(":compiler:generateTestData")
}

projectTest(parallel = true) {
    workingDir = rootDir
    setupV8()
    setupWasmStdlib()
    setupGradlePropertiesForwarding()
    systemProperty("kotlin.wasm.test.root.out.dir", "$buildDir/")
}