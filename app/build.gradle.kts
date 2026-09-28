plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.rexaps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rexaps"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")

    implementation("com.github.mwiede:jsch:0.2.18")

    // Compose BOM: aligns every androidx.compose.* artifact below
    // to one mutually-compatible version set. Do not add explicit
    // version numbers to any androidx.compose.* line below this.
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))

    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Fixes: Unresolved reference 'viewModel' / 'compose'
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // Used by RexNuxViewModel/RexNuxTerminalViewModel's
    // viewModelScope.launch(Dispatchers.IO) calls
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // ... dependency lu yang udah ada tetap ...

    // Compose BOM sudah ada, jadi jangan tambah versi di bawah ini
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // image loader
    implementation("io.coil-kt:coil-compose:2.6.0")
}
