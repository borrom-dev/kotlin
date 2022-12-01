// DO NOT EDIT MANUALLY!
// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
// To regenerate run 'generateGradleOptions' task
@file:Suppress("RemoveRedundantQualifierName", "Deprecation", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dsl

enum class KotlinVersion(val version: String) {
    @Deprecated("Unsupported", level = DeprecationLevel.ERROR) KOTLIN_1_0("1.0"),
    @Deprecated("Unsupported", level = DeprecationLevel.ERROR) KOTLIN_1_1("1.1"),
    @Deprecated("Unsupported", level = DeprecationLevel.ERROR) KOTLIN_1_2("1.2"),
    @Deprecated("Unsupported", level = DeprecationLevel.ERROR) KOTLIN_1_3("1.3"),
    @Deprecated("Will be removed soon") KOTLIN_1_4("1.4"),
    @Deprecated("Will be removed soon") KOTLIN_1_5("1.5"),
    KOTLIN_1_6("1.6"),
    KOTLIN_1_7("1.7"),
    KOTLIN_1_8("1.8"),
    KOTLIN_1_9("1.9"),
    KOTLIN_2_0("2.0"),
    ;

    companion object {
        fun fromVersion(version: String): KotlinVersion =
            KotlinVersion.values().firstOrNull { it.version == version }
                ?: throw IllegalArgumentException("Unknown Kotlin version: $version")
    }
}
