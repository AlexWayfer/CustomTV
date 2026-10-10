import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
        versionCode = 19

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val twitchClientId = Properties().apply {
            val file = rootProject.file("twitch.properties")
            if (file.exists()) file.inputStream().use(::load)
        }.getProperty("TWITCH_CLIENT_ID").orEmpty()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "TWITCH_CLIENT_ID", "\"$twitchClientId\"")
        val telegram = Properties().apply {
            val file = rootProject.file("telegram.properties")
            if (file.exists()) file.inputStream().use(::load)
        }
        val telegramApiId = telegram.getProperty("TELEGRAM_API_ID")?.toIntOrNull() ?: 0
        val telegramApiHash = telegram.getProperty("TELEGRAM_API_HASH").orEmpty()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        val telegramOpenChatId = telegram.getProperty("TELEGRAM_OPEN_CHAT_ID")?.toLongOrNull() ?: 0L
        val telegramPremiumChatId = telegram.getProperty("TELEGRAM_PREMIUM_CHAT_ID")?.toLongOrNull() ?: 0L
        val telegramOpenTopicId = telegram.getProperty("TELEGRAM_OPEN_TOPIC_ID")?.toIntOrNull() ?: 0
        val telegramPremiumTopicId = telegram.getProperty("TELEGRAM_PREMIUM_TOPIC_ID")?.toIntOrNull() ?: 0
        buildConfigField("int", "TELEGRAM_API_ID", "$telegramApiId")
        buildConfigField("String", "TELEGRAM_API_HASH", "\"$telegramApiHash\"")
        buildConfigField("long", "TELEGRAM_OPEN_CHAT_ID", "${telegramOpenChatId}L")
        buildConfigField("long", "TELEGRAM_PREMIUM_CHAT_ID", "${telegramPremiumChatId}L")
        buildConfigField("int", "TELEGRAM_OPEN_TOPIC_ID", "$telegramOpenTopicId")
        buildConfigField("int", "TELEGRAM_PREMIUM_TOPIC_ID", "$telegramPremiumTopicId")
    }

    flavorDimensions += "tier"
    productFlavors {
        create("free") {
            dimension = "tier"
            versionName = "1.6.0"
            buildConfigField("boolean", "PREMIUM", "false")
        }
        if (premiumSources) {
            create("premium") {
                dimension = "tier"
                isDefault = true
                versionName = "1.5.0"
                buildConfigField("boolean", "PREMIUM", "true")
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
            optimization {
                enable = true
                keepRules {
                    files.add(file("proguard-rules.pro"))
                }
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
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.browser)
    implementation(libs.tdlib.android.core)
    implementation(libs.androidx.work.runtime)
    if (premiumSources) {
        // Drag to reorder the chatter labels in the settings.
        "premiumImplementation"(libs.reorderable)
    }
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Names composables in a Perfetto trace of a debug build.
    debugImplementation(libs.androidx.compose.runtime.tracing)
    debugImplementation(libs.androidx.tracing.perfetto.binary)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
