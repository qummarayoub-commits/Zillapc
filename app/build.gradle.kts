import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Load TMDB key from local.properties (never committed) with a CI env-var fallback,
// so the key can be supplied either locally or via a GitHub Actions secret.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { input -> load(input) }
}
val tmdbApiKey: String = (localProperties.getProperty("TMDB_API_KEY")
    ?: System.getenv("TMDB_API_KEY")
    ?: "")
val comicVineApiKey: String = (localProperties.getProperty("COMICVINE_API_KEY")
    ?: System.getenv("COMICVINE_API_KEY")
    ?: "")
val omdbApiKey: String = (localProperties.getProperty("OMDB_API_KEY")
    ?: System.getenv("OMDB_API_KEY")
    ?: "")

android {
    namespace = "com.darkjade.streamlib"
    // Bumped 34 -> 36: media3 1.11.0's AAR metadata requires compiling
    // against API 35+ (AGP 8.13's max supported level is 36.1) - the actual
    // cause of the checkDebugAarMetadata failure when the Media3 upgrade
    // first landed.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.darkjade.streamlib"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "COMICVINE_API_KEY", "\"$comicVineApiKey\"")
        buildConfigField("String", "OMDB_API_KEY", "\"$omdbApiKey\"")

        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel + Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Room
    // Bumped 2.6.1 -> 2.8.0 (latest 2.x - NOT the breaking Room 3.0 line,
    // which changes package/artifact IDs and requires all-suspend DAOs):
    // fixes a real KSP2 bug ("unexpected jvm signature V" on Room DAO
    // methods) that Kotlin 2.2/KSP 2.2.10-2.0.2 triggers with Room 2.6.1.
    implementation("androidx.room:room-runtime:2.8.0")
    implementation("androidx.room:room-ktx:2.8.0")
    ksp("androidx.room:room-compiler:2.8.0")

    // DataStore (settings)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager (background scanning)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")
    // For extracting a dominant color from poster/backdrop artwork to tint
    // the details-page background — a well-known, stable AndroidX library.
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Media3 / ExoPlayer — internal video player
    // Bumped 1.4.1 -> 1.11.0: real fix for MP4/MKV seeking. Upstream media3
    // had a known MatroskaExtractor bug where Cue points weren't correctly
    // associated with their track for multi-track files, and DefaultExtractorsFactory's
    // handling of MP4/MKV seek maps has had several accuracy fixes since 1.4.1
    // (release notes: "Fix an issue in MatroskaExtractor where seeking could be
    // inaccurate for files with multiple tracks"). This is the actual root cause
    // behind "seek works on some files, not others" for real local movie files -
    // the earlier setConstantBitrateSeekingEnabled fix only ever applied to
    // MP3/ADTS/AMR audio containers, never to MP4/MKV video containers.
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation("androidx.media3:media3-common:1.11.0")
    implementation("androidx.media3:media3-session:1.11.0")

    // FFmpeg audio decoder extension — built via a dedicated GitHub Actions
    // workflow (ffmpeg-extension-build.yml), not a Maven dependency (Google
    // doesn't publish this one due to codec licensing). Adds real AC3/E-AC3/
    // DTS-Core playback via software decoding when the device's own hardware
    // codec can't handle a track — DefaultRenderersFactory's
    // EXTENSION_RENDERER_MODE_PREFER (already configured in PlayerViewModel)
    // discovers androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer via
    // reflection automatically once it's on the classpath; no other app-side
    // wiring is required for it to be picked up.
    implementation(project(":decoder-ffmpeg"))

    // Internal comic reader — CBZ (zip) via java.util.zip (built-in), CBR (rar) via junrar
    implementation("com.github.junrar:junrar:7.5.2")

    // Networking (metadata provider)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    implementation("androidx.paging:paging-compose:3.3.0")
    implementation("androidx.room:room-paging:2.8.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
