plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.comntrol.quicktimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.comntrol.quicktimer"
        minSdk = 30
        targetSdk = 34
        versionCode = 3
        versionName = "2.1"
    }

    // Chave fixa: todo build (local ou GitHub) sai com a mesma assinatura,
    // então "adb install -r" atualiza sem precisar desinstalar.
    signingConfigs {
        create("fixed") {
            storeFile = file("quicktimer.jks")
            storePassword = "quicktimer"
            keyAlias = "quicktimer"
            keyPassword = "quicktimer"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.wear:wear-ongoing:1.0.0")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
}
