import plugins.KotlinBuildPublishingPlugin.Companion.ADHOC_COMPONENT_NAME

plugins {
    kotlin("jvm")
    `java-test-fixtures`
    `maven-publish`
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin.sourceSets.configureEach {
    languageSettings.apiVersion = "1.4"
    languageSettings.languageVersion = "1.4"
    languageSettings.optIn("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
}

dependencies {
    api(project(":kotlin-tooling-core"))
    api(project(":kotlin-gradle-plugin-annotations"))
    compileOnly(kotlinStdlib())
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation(project(":kotlin-gradle-plugin"))
    testImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))

    testImplementation("org.reflections:reflections:0.10.2") {
        because("Tests on the object graph are performed. This library will find implementations of interfaces at runtime")
    }

    testFixturesImplementation(gradleApi())
    testFixturesImplementation(gradleKotlinDsl())
    testFixturesImplementation(project(":kotlin-tooling-core"))
    testFixturesImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    testFixturesImplementation(project(":kotlin-test:kotlin-test-junit"))
}


publish(moduleMetadata = true) {
    fun ConfigurationVariantDetails.skipUnpublishable() {
        if (configurationVariant.artifacts.any { JavaBasePlugin.UNPUBLISHABLE_VARIANT_ARTIFACTS.contains(it.type) }) {
            skip()
        }
    }

    suppressAllPomMetadataWarnings()

    val kotlinLibraryComponent = components[ADHOC_COMPONENT_NAME] as AdhocComponentWithVariants

    kotlinLibraryComponent.addVariantsFromConfiguration(configurations.testFixturesApiElements.get()) {
        skipUnpublishable()
        mapToMavenScope("compile")
        mapToOptional()
    }

    kotlinLibraryComponent.addVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) {
        skipUnpublishable()
        mapToMavenScope("runtime")
        mapToOptional()
    }
}

javadocJar()
sourcesJar()

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}

//region Setup: Backwards compatibility tests

run {
    val compatibilityTestClasspath by configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }

    dependencies {
        compatibilityTestClasspath(project(":kotlin-gradle-plugin-idea-for-compatibility-tests"))
    }

    tasks.test {
        dependsOnKotlinGradlePluginInstall()
        dependsOn(compatibilityTestClasspath)
        val conf: FileCollection = compatibilityTestClasspath
        inputs.files(conf)
        doFirst { systemProperty("compatibilityTestClasspath", conf.files.joinToString(";") { it.absolutePath }) }
    }
}

//endregion
