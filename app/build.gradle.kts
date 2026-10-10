
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rexaps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rexaps"
        minSdk = 27
        targetSdk = 35
        versionCode = 2693
        versionName = "26.93"
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

    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/INDEX.LIST",
                "META-INF/versions/**",
                "plugin.properties",
                "about.html"
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    // GeckoView (Mozilla Gecko engine)
    implementation "org.mozilla.geckoview:geckoview-stable:132.0.2"

    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // SSH
    implementation("com.github.mwiede:jsch:0.2.18")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))

    // Compose
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Tunnel / VPN
    implementation("com.wireguard.android:tunnel:1.0.20230706")

    // Room
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // RexArchive: ZIP, TAR, GZIP, BZIP2, XZ, 7z backends
    implementation("org.apache.commons:commons-compress:1.26.2")
    implementation("org.tukaani:xz:1.9")

    // RexArchive: RAR listing/extraction only
    implementation("com.github.junrar:junrar:7.5.5")

    // SLF4J
    implementation("org.slf4j:slf4j-nop:1.7.36")

    // PDF rendering uses Android's built-in PdfRenderer.
    // PDF text extraction needs a separate backend if required.

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image loader
    implementation("io.coil-kt:coil-compose:2.6.0")

    // JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.3.202401111512-r")

    // Media Exoplayer
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Test
    testImplementation("junit:junit:4.13.2")
}
