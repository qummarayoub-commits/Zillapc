plugins {
    id("com.android.library")
}

android {
    namespace = "androidx.media3.decoder.ffmpeg"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        externalNativeBuild {
            cmake {
                // Match cmake_minimum_required in CMakeLists.txt.
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
        ndk {
            // Only build for the ABIs we actually compiled FFmpeg static libs for.
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    // Only configure the native build if the prebuilt FFmpeg libraries are
    // actually present — mirrors the upstream module's own guard so a fresh
    // checkout without the compiled libs doesn't break Gradle sync.
    if (project.file("src/main/jni/ffmpeg/android-libs").exists()) {
        externalNativeBuild {
            cmake {
                path = file("src/main/jni/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Published Maven artifacts — this module is built standalone against
    // Media3's public API, not as part of the androidx/media monorepo
    // (which uses internal project() references we don't have).
    implementation("androidx.media3:media3-decoder:1.4.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("androidx.annotation:annotation:1.8.0")
    compileOnly("org.checkerframework:checker-qual:3.42.0")
}
