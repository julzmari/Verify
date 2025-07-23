
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    id("kotlin-kapt")
    id("kotlin-parcelize")

}

android {
    namespace = "com.mobdeve.s18.verify"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mobdeve.s18.verify"
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
    kotlinOptions {
        jvmTarget = "11"
    }


}

val cameraxVersion = "1.3.0"

dependencies {
    implementation("io.github.jan-tennert.supabase:gotrue-kt:1.4.2")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:1.4.2")
    implementation("io.github.jan-tennert.supabase:storage-kt:1.4.2")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("org.mindrot:jbcrypt:0.4")


    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("com.google.android.material:material:1.12.0")


    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.play.services.maps)
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation ("org.osmdroid:osmdroid-android:6.1.10")
    implementation(libs.material)
    testImplementation(libs.junit)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // Location services
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}
