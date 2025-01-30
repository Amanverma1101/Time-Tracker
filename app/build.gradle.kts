import com.android.build.api.dsl.Packaging

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
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0"
            )
        }
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
    implementation("com.google.api-client:google-api-client-android:1.31.5")
    implementation("com.google.android.gms:play-services-auth:20.1.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev581-1.25.0")
    implementation("com.google.api-client:google-api-client:1.31.5")
    implementation("com.google.api-client:google-api-client-jackson2:1.31.5")
    implementation("com.google.api-client:google-api-client-gson:1.31.5") // Use Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.ibm.icu:icu4j:70.1")//for emoji check
    implementation("com.google.android.material:material:1.4.0")




//    implementation("androidx.activity:activity:1.2.0")
//    implementation("androidx.fragment:fragment:1.3.0")





    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


}
