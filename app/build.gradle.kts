plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
//    id("kotlin-parcelize")
//    id("kotlin-kapt")
}

android {
    buildFeatures {
        viewBinding = true
    }
    namespace = "com.example.everynewsapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.everynewsapp"
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

dependencies {
    implementation("org.jsoup:jsoup:1.21.2")

    // Room (버전 카탈로그 사용)
    implementation("androidx.room:room-runtime:2.6.1") // 또는 "androidx.room:room-runtime:2.6.1"
    kapt("androidx.room:room-compiler:2.6.1")
    implementation(libs.androidx.room.ktx)

    // ViewModel & LiveData (Lifecycle)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")

    implementation(libs.coil) // 이미지 로딩 라이브러리 (Coil 선택)
    // implementation("com.github.bumptech.glide:glide:4.16.0") // Glide 사용 안 할 경우 제거

    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

