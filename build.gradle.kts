buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.google.gms:google-services:4.3.15")  // Check for the latest version if necessary
        // Add other classpaths like Kotlin if needed
    }
}


plugins {
    alias(libs.plugins.android.application) apply false
    // You can add other plugins here if needed, like Kotlin
}

// Here you can add configurations that apply to all subprojects, if any
