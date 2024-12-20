plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    kotlin("kapt")
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace = "com.example.tokoplastik"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tokoplastik"
        minSdk = 26
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.hiltAndroid)
    kapt(libs.hiltCompiler)
    implementation(libs.retrofit)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)
    implementation(libs.gson)
    implementation(libs.lifecycle)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.datastore.preferences)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.livedata)
    implementation(libs.recyclerview)
    implementation(libs.swipe.refresh.layout)
    implementation(libs.sweet.alert)
    implementation(libs.itext.pdf)
    implementation(libs.mp.chart)
}