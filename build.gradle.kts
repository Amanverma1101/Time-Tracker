// Apply the Android plugin for your project
plugins {
    alias(libs.plugins.android.application) apply false
    // You can add other plugins here if needed, like Kotlin
}

buildscript {
    repositories {
        google()  // Make sure to add Google Maven repository
        mavenCentral()
    }
    dependencies {
        // Add the Google services plugin classpath
        classpath("com.google.gms:google-services:4.3.15")  // Firebase services
    }
}

// Additional configurations can go here
