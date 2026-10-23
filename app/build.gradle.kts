import dev.detekt.gradle.Detekt
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

// The public repository carries the free edition only; without the premium sources only free builds.
val premiumSources = file("src/premium").isDirectory

android {
    namespace = "name.alexwayfer.customtv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "name.alexwayfer.customtv"
        minSdk = 28
        targetSdk = 37
        // One counter for both editions: they share the application ID and install over each other, and
        // Android refuses a lower code. Every release of either edition raises it; each edition names its own version.
        versionCode = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val twitchClientId = Properties().apply {
            val file = rootProject.file("twitch.properties")
            if (file.exists()) file.inputStream().use(::load)
        }.getProperty("TWITCH_CLIENT_ID").orEmpty()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "TWITCH_CLIENT_ID", "\"$twitchClientId\"")
    }

    flavorDimensions += "tier"
    productFlavors {
        create("free") {
            dimension = "tier"
            versionName = "1.12.0"
            buildConfigField("boolean", "PREMIUM", "false")
        }
        if (premiumSources) {
            create("premium") {
                dimension = "tier"
                isDefault = true
                versionName = "1.11.1"
                buildConfigField("boolean", "PREMIUM", "true")
                // Premium logs in to Telegram: its group gives the access and the updates.
                val telegram = Properties().apply {
                    val file = rootProject.file("telegram.properties")
                    if (file.exists()) file.inputStream().use(::load)
                }
                val telegramApiId = telegram.getProperty("TELEGRAM_API_ID")?.toIntOrNull() ?: 0
                val telegramApiHash = telegram.getProperty("TELEGRAM_API_HASH").orEmpty()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                val telegramPremiumChatId = telegram.getProperty("TELEGRAM_PREMIUM_CHAT_ID")?.toLongOrNull() ?: 0L
                val telegramPremiumTopicId = telegram.getProperty("TELEGRAM_PREMIUM_TOPIC_ID")?.toIntOrNull() ?: 0
                buildConfigField("int", "TELEGRAM_API_ID", "$telegramApiId")
                buildConfigField("String", "TELEGRAM_API_HASH", "\"$telegramApiHash\"")
                buildConfigField("long", "TELEGRAM_PREMIUM_CHAT_ID", "${telegramPremiumChatId}L")
                buildConfigField("int", "TELEGRAM_PREMIUM_TOPIC_ID", "$telegramPremiumTopicId")
            }
        }
    }
    // The release key stays outside git: keystore.properties names the keystore and its passwords.
    // Without it a release build is left unsigned, so a debug-signed APK cannot be published by mistake.
    val releaseKey = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use(::load)
    }
    signingConfigs {
        releaseKey.getProperty("storeFile")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = releaseKey.getProperty("storePassword")
                keyAlias = releaseKey.getProperty("keyAlias")
                keyPassword = releaseKey.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug {
            // Debug and release builds carry different keys, so they install side by side instead of replacing each other.
            applicationIdSuffix = ".debug"
            ndk {
                abiFilters += "arm64-v8a"
            }
        }
        release {
            // R8 shrinks the APK and renames the code, so the Premium checks are harder to find and patch out.
            // Its rules are in src/main/keepRules.
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
            // Some phones with a 64-bit CPU run a 32-bit Android, such as the Galaxy A10, and refuse an APK
            // without armeabi-v7a libraries.
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Android 13+ lists the languages of the values-* folders in the app's system language screen.
    androidResources {
        generateLocaleConfig = true
    }
    // A unit test of code that logs through AppLog reaches android.util.Log, which only answers on a device.
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric reads the app's resources and merged manifest.
        unitTests.isIncludeAndroidResources = true
        // Robolectric reaches into the JDK's internals, which Java 17 and newer close (robolectric.org/getting-started).
        unitTests.all {
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            )
        }
    }
}

// A release APK is named for its post in the edition's Telegram topic: CustomTV-free-1.0.3.apk.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(output.versionName.map { "CustomTV-${variant.flavorName}-$it.apk" })
        }
    }
}

kotlin {
    compilerOptions {
        extraWarnings.set(true)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.browser)
    if (premiumSources) {
        // Telegram for the Premium access and updates; its native library is most of an APK's size.
        "premiumImplementation"(libs.tdlib.android.core)
    }
    implementation(libs.androidx.work.runtime)
    // Drag to reorder the chatter labels in the settings.
    implementation(libs.reorderable)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Names composables in a Perfetto trace of a debug build.
    debugImplementation(libs.androidx.compose.runtime.tracing)
    debugImplementation(libs.androidx.tracing.perfetto.binary)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    // Android and Compose on the JVM, so tests can render a composable and touch it on a virtual clock.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Compose's tests wait on Espresso, whose version they bring (3.5) fails on Android 17's input manager.
    testImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
}

// verify.ps1 -Detekt lists the changed Kotlin files in this file, one path a line: each variant then analyzes only
// its own of them, and resolves the types of the other files from the variant's compiled classes. Without the
// property, as before a commit, every variant analyzes all its sources.
val detektFiles = providers.gradleProperty("detektFilesList")
    .flatMap { providers.fileContents(rootProject.layout.projectDirectory.file(it)).asText }
    .orNull
    ?.lines()
    ?.filter { it.isNotBlank() }
    ?.map { file(it.trim()) }
    ?.toSet()
if (detektFiles != null) {
    // An exclude, not a new source: the plugin adds the variant's sources after this action runs.
    tasks.withType<Detekt>().configureEach {
        // A local copy: the configuration cache cannot store a reference to the build script.
        val analyzed = detektFiles
        exclude { !it.isDirectory && it.file !in analyzed }
        // The plugin's classpath lacks the variant's own classes with AGP's built-in Kotlin; a test variant gets
        // the tested classes from its libraries. An explicit value replaces the plugin's, so each names both. The
        // classes are friends, as the code is mostly internal.
        val compileName = name.replaceFirst("detekt", "compile") + "Kotlin"
        if (compileName in tasks.names) {
            val compile = tasks.named<KotlinJvmCompile>(compileName)
            val classes = compile.flatMap { it.destinationDirectory }
            classpath.from(compile.map { it.libraries }, classes)
            friendPaths.from(compile.map { it.friendPaths }, classes)
        }
    }
}
