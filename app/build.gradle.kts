@file:Suppress("DEPRECATION")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.qihe.clipflow"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.qihe.clipflow"
        minSdk = 29
        targetSdk = 37
        versionCode = 62
        versionName = "3.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
            useVersion("2.3.10")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // Umeng Analytics
    implementation("com.umeng.umsdk:common:9.9.2")
    implementation("com.umeng.umsdk:asms:1.8.2")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Media3 ExoPlayer (应用内视频播放)
    implementation("androidx.media3:media3-exoplayer:1.10.0")
    implementation("androidx.media3:media3-ui:1.10.0")
    implementation("androidx.media3:media3-common:1.10.0")
    implementation("androidx.media3:media3-ui-compose:1.10.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")

    // Progressive backdrop blur
    implementation("io.github.kyant0:backdrop:2.0.0")
    implementation("io.github.kyant0:shapes:1.2.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Browser (CustomTabs)
    implementation("androidx.browser:browser:1.8.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // QR code rendering for the Bilibili web login URL
    implementation("com.google.zxing:core:3.5.3")

    // Accompanist (system UI controller for edge-to-edge)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
