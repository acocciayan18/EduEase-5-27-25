plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.eduease"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.eduease"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // AndroidX and Material Design
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase (BOM ensures consistent versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Google Play Services
    implementation(libs.play.services.auth)

    // Image Loading and Processing
    implementation(libs.glide)
    implementation(libs.recyclerview)
    implementation(libs.firebase.database)
    implementation(libs.ui.graphics.android)
    annotationProcessor(libs.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.cardview)

    implementation(libs.generativeai)
//    implementation (libs.google.cloud.language)
//    implementation (libs.example.generativeai)
    implementation (libs.gson)
    implementation (libs.okhttp)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.konfetti.xml)
    implementation (libs.konfetti.core)


        // Import the BoM for the Firebase platform
        implementation(libs.firebase.bom.v33130)

        // Add the dependency for the Firebase Authentication library
        // When using the BoM, you don't specify versions in Firebase library dependencies
        implementation(libs.com.google.firebase.firebase.auth)

        // Also add the dependency for the Google Play services library and specify its version
        implementation(libs.play.services.auth)









}
