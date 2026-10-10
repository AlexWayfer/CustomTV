// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    idea
}

// Local Gradle, Android, and Kotlin homes used by scripts/verify.ps1; keep them out of indexing and inspections.
idea.module.excludeDirs.addAll(files(".gradle-user", ".android-user", ".kotlin"))
