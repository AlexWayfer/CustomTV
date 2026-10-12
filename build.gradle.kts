import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // dependencyUpdates lists newer versions of the dependencies and plugins; scripts\outdated.ps1 runs it.
    alias(libs.plugins.versions)
    idea
}

// Local Gradle, Android, and Kotlin homes used by scripts/verify.ps1; keep them out of indexing and inspections.
idea.module.excludeDirs.addAll(files(".gradle-user", ".android-user", ".kotlin"))

// A stable version is offered only stable updates; a pre-release, anything newer.
tasks.withType<DependencyUpdatesTask>().configureEach {
    rejectVersionIf { isPreRelease(candidate.version) && !isPreRelease(currentVersion) }
    gradleReleaseChannel = "current"
    // Only the classpaths that hold what the version catalog declares. Resolving all of the dozens of internal
    // configurations (Kotlin's own, lint's, both flavors' variants) is what makes the check slow.
    filterConfigurations = Spec { it.name in checkedConfigurations }
}

// Premium holds everything free does, plus its own dependencies.
val checkedConfigurations = setOf(
    "premiumDebugRuntimeClasspath",
    "premiumDebugUnitTestRuntimeClasspath",
    "premiumDebugAndroidTestRuntimeClasspath",
)

fun isPreRelease(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    return !stableKeyword && !Regex("^[0-9,.v-]+(-r)?$").matches(version)
}
