plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.kapt")
}

configurations.configureEach {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

// GeckoView Nightly 149 pulls AndroidX Core 1.17.0 (compileSdk 36).
// This app remains on compileSdk 35, so preserve the project's explicit Core 1.15.0 compatibility pin.
configurations.all {
    resolutionStrategy {
        force(
            "androidx.core:core:1.15.0",
            "androidx.core:core-ktx:1.15.0",
            // Tambahkan ini untuk menimpa kotlin-stdlib dari GeckoView
            "org.jetbrains.kotlin:kotlin-stdlib:2.0.21",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21"
        )
    }
}
// ================================================

val releaseStoreFilePath = providers.environmentVariable("RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
    releaseStoreFilePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { !it.isNullOrBlank() }

// The current Release build has a runtime NoSuchMethodException in RexGit that
// appears only after R8 shrinking/obfuscation. Keep Release runnable by default;
// minification can be re-enabled explicitly after validating the keep rules with
// a retraced release stack trace: -Prexaps.release.minify=true.
val releaseMinificationEnabled = providers.gradleProperty("rexaps.release.minify")
    .map { it.equals("true", ignoreCase = true) }
    .getOrElse(false)

android {
    namespace = "com.rexaps"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    // Work around a crash in the Lifecycle NullSafeMutableLiveData lint detector
    // reported by lintVitalAnalyzeRelease. Other lint checks remain enabled.
    lint {
        disable += "NullSafeMutableLiveData"
    }

    defaultConfig {
        applicationId = "com.rexaps"
        minSdk = 27
        targetSdk = 35
        versionCode = 2726
        versionName = "27.26"

        // Ship both supported ARM ABIs. The Node.js Mobile runtime is added
        // by CI for each ABI before the Release variant is assembled.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    // Produce two device-specific APKs plus one fat APK containing both ARM ABIs.
    // Keep this list aligned with defaultConfig.ndk.abiFilters and the Node.js
    // Mobile native libraries downloaded in .github/workflows/android.yml.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = releaseMinificationEnabled
            isShrinkResources = releaseMinificationEnabled
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // CI injects a persistent signing key from GitHub Actions secrets.
            // Local builds remain possible but are unsigned until these variables are supplied.
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
        jniLibs {
            useLegacyPackaging = false
            // Some Android AARs may also package this runtime; retain the
            // exact libc++_shared.so shipped beside the RexChat Node binary.
            pickFirsts += "**/libc++_shared.so"
        }
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

    implementation("org.apache.commons:commons-compress:1.26.1")
    implementation("org.tukaani:xz:1.9")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.16.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("com.github.mwiede:jsch:0.2.18")

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

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.wireguard.android:tunnel:1.0.20230706")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.github.junrar:junrar:7.5.5")

    implementation("org.slf4j:slf4j-nop:1.7.36")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.zxing:core:3.5.3")

    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("io.noties.markwon:image:4.6.2")
    // Markwon's SVG/GIF decoders reference these optional Android libraries.
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.32")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:syntax-highlight:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:ext-tasklist:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties:prism4j:2.0.0")
    kapt("io.noties:prism4j-bundler:2.0.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.3.202401111512-r")

    testImplementation("junit:junit:4.13.2")

    implementation("androidx.core:core-splashscreen:1.0.1")

    // GeckoView engine used only by RexFox and RexAI. Keep the validated project pin.
    implementation("org.mozilla.geckoview:geckoview-nightly:149.0.20260223091613")
}