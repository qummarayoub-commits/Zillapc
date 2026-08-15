import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

android {
    namespace = "com.darkjade.streamlib"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.darkjade.streamlib"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "COMICVINE_API_KEY", "\"$comicVineApiKey\"")

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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (settings)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager (background scanning)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    // Media3 / ExoPlayer — internal video player
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")

    // Internal comic reader — CBZ (zip) via java.util.zip (built-in), CBR (rar) via junrar
    implementation("com.github.junrar:junrar:7.5.5")

    // Networking (metadata provider)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    implementation("androidx.paging:paging-compose:3.3.0")
    implementation("androidx.room:room-paging:2.6.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
