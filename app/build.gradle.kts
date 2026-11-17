plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.nowlsantos.big2ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nowlsantos.big2ai"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = project.findProperty("KEYSTORE_FILE") as? String
                ?: System.getenv("KEYSTORE_FILE")
                ?: "C:\\Users\\Noel Santos\\Documents\\kotlin\\keyStore\\big2aiks.jks"
            val keystoreFile = file(keystorePath)

            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = project.findProperty("KEYSTORE_PASSWORD") as? String
                    ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = project.findProperty("KEY_ALIAS") as? String
                    ?: System.getenv("KEY_ALIAS") ?: ""
                keyPassword = project.findProperty("KEY_PASSWORD") as? String
                    ?: System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG_MODE", "true")

            isMinifyEnabled = false
        }
        release {
            // Enables code shrinking, obfuscation, and optimization for release builds.
            isMinifyEnabled = true

            // Enables resource shrinking, removing unused resources. Requires isMinifyEnabled=true.
            isShrinkResources = true

            // Specifies the ProGuard rules files.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Reference the signing configuration if valid keystore file exists
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning?.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }

            // Optional: Define release-specific BuildConfig fields if needed
            buildConfigField("boolean", "DEBUG_MODE", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    // To use constraintlayout in compose
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // ViewModel extensions for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // This provides ViewModel injections and hiltViewModel() for Compose
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}