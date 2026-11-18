plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val legacyStoreFile = providers.environmentVariable("WQ_UPGRADE_KEYSTORE").orNull
val legacyStorePassword = providers.environmentVariable("WQ_UPGRADE_STORE_PASSWORD").orNull
val legacyKeyAlias = providers.environmentVariable("WQ_UPGRADE_KEY_ALIAS").orNull
val legacyKeyPassword = providers.environmentVariable("WQ_UPGRADE_KEY_PASSWORD").orNull
val hasLegacySigning = listOf(legacyStoreFile, legacyStorePassword, legacyKeyAlias, legacyKeyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "com.kelimebulmacasi.a1"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.kelimebulmacasi.a1.practice"
        minSdk = 26
        targetSdk = 35
        versionCode = 73
        versionName = "1.0.48-neon-word-memory"
        manifestPlaceholders["arenaScheme"] = "wordquestai"
        buildConfigField("String", "ARENA_SCHEME", "\"wordquestai\"")
    }

    signingConfigs {
        if (hasLegacySigning) create("legacy075") {
            storeFile = file(requireNotNull(legacyStoreFile))
            storePassword = legacyStorePassword
            keyAlias = legacyKeyAlias
            keyPassword = legacyKeyPassword
        }
    }

    buildTypes {
        getByName("release") {
            // Preserve update signing when the owner supplies the existing private key.
            // Without the key, produce an unsigned APK rather than silently switching identity.
            if (hasLegacySigning) signingConfig = signingConfigs.getByName("legacy075")
        }
        // Independent install variant: will not replace .test or normal v0.6.0 installations.
        create("offline") {
            initWith(getByName("debug"))
            // Independent installation as requested: does not replace v0.8.2 or v1.0.0 preview.
            applicationIdSuffix = ".fresh"
            // Build unsigned in CI, then sign locally; no user private key in GitHub.
            signingConfig = null
            matchingFallbacks += "debug"
            manifestPlaceholders["arenaScheme"] = "wordquestai-offline101"
            buildConfigField("String", "ARENA_SCHEME", "\"wordquestai-offline082\"")
        }
        create("preview") {
            initWith(getByName("debug"))
            // The supplied v0.7.5 APK is the .test variant. Keep its package unchanged.
            // Without the original key, preview builds are NOT upgrade-compatible.
            if (hasLegacySigning) signingConfig = signingConfigs.getByName("legacy075")
            applicationIdSuffix = ".test"
            matchingFallbacks += "debug"
            manifestPlaceholders["arenaScheme"] = "wordquestai-test"
            buildConfigField("String", "ARENA_SCHEME", "\"wordquestai-test\"")
        }
    }

    // Robolectric requires packaged Android assets to validate advanced Arena boards.
    testOptions { unitTests.isIncludeAndroidResources = true }

    // MediaPlayer's AssetFileDescriptor requires raw (uncompressed) audio entries in APK.
    androidResources {
        noCompress += setOf("ogg")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-auth:23.2.1")
    implementation("com.google.firebase:firebase-firestore:25.1.4")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
