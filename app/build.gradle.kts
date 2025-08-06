plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.progetto_tosa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.progetto_tosa"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    // ————— ANT+ SDK (AAR + JAR) —————
    // L’AAR sarà risolto via flatDir definito in settings.gradle.kts
    implementation(files("libs/ANT-Android-SDKs/ANT+_Android_SDK/API/antpluginlib_3-9-0.aar"))
    implementation(
        fileTree(
            mapOf(
                "dir" to "libs/ANT-Android-SDKs/ANT+_Android_SDK/API",
                "include" to listOf("*.jar")
            )
        )
    )

    /* --- Desugaring --- */
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    /* --- Room (KSP) --- */
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    /* --- MPAndroidChart --- */
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    /* --- AndroidX core / UI --- */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation("androidx.transition:transition:1.4.1")
    implementation(libs.androidx.activity)
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    /* --- Lifecycle & Navigation --- */
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    /* --- Firebase --- */
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation(libs.firebase.auth.ktx)
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.firebaseui:firebase-ui-auth:9.0.0")

    /* --- Effetti UI --- */
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    /* --- Kotlin reflect --- */
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")

    /* --- WorkManager --- */
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    /* --- Coroutines --- */
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    /* --- ZXing --- */
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.1")

    /* --- Swipe-to-refresh --- */
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    /* --- Retrofit / OkHttp / Gson --- */
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    /* --- Test --- */
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
