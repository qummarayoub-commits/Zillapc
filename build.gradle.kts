// Top-level build file
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    // Bumped 1.9.24 -> 2.2.10: Media3 1.11.0 pulls in kotlin-stdlib 2.2.10
    // transitively, and Kotlin's metadata is forward-incompatible (a module
    // compiled with 2.2 can't be read by a 1.9 compiler) - this caused
    // "Module was compiled with an incompatible version of Kotlin" on
    // :app:kspDebugKotlin. Kotlin 2.0+ also moves Compose's compiler out of
    // the `composeOptions.kotlinCompilerExtensionVersion` mechanism and into
    // its own Gradle plugin, applied below and in app/build.gradle.kts.
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
