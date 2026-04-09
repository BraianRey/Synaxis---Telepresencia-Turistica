plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ----------------------------------------------
// 1. Read local.properties (if exists)
// ----------------------------------------------
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// ----------------------------------------------
// 2. Android Configuration
// ----------------------------------------------
android {
    namespace = "com.sismptm.client"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sismptm.client"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ----- Environment variables from local.properties -----
        val baseUrlApi: String = localProperties.getProperty("BASE_URL_API") ?: ""
        val baseUrlKeycloak: String = localProperties.getProperty("BASE_URL_KEYCLOAK") ?: ""
        val baseWebrtc: String = localProperties.getProperty("BASE_WEBRTC") ?: ""

        buildConfigField("String", "BASE_URL_API", "\"$baseUrlApi\"")
        buildConfigField("String", "BASE_URL_KEYCLOAK", "\"$baseUrlKeycloak\"")
        buildConfigField("String", "BASE_WEBRTC", "\"$baseWebrtc\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

// ----------------------------------------------
// 3. Dependencies
// ----------------------------------------------
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Retrofit & OkHttp
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // WebRTC
    implementation(libs.webrtc)

    // ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}