plugins {
    id("com.android.application")  // Android application plugin
    id("com.google.gms.google-services")// Correct usage of the Google Services plugin
}


android {
    namespace = "com.example.timetracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.timetracker"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Android UI dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase SDKs
    implementation("com.google.firebase:firebase-database:20.0.6")  // Firebase Realtime Database
    implementation("com.google.firebase:firebase-auth:21.1.0")  // Firebase Authentication (optional)
    implementation("com.google.firebase:firebase-analytics:21.2.0")  // Firebase Analytics (optional)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


}
