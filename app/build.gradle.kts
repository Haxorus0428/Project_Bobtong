plugins {
    id("com.android.application")
    // Google 서비스 Gradle 플러그인에 대한 종속성 추가
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.project_bobtong"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.project_bobtong"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    // 앱 모듈 build.gradle
    // 네이버 지도 SDK
    implementation("com.naver.maps:map-sdk:3.18.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // retrofit2 http라이브러리 , gson 컨버터
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // recyclerview
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    // Firebase BoM 가져오기
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))

    // Firebase Auth
    implementation("com.google.firebase:firebase-auth:21.0.1")

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database")

    // Firebase Analytics
    implementation("com.google.firebase:firebase-analytics")

    // 사용자 편의성 증가용 슬라이드 업 패널
    implementation("com.sothree.slidinguppanel:library:3.4.0")

    // 필요한 다른 Firebase 제품의 종속성 추가
    // https://firebase.google.com/docs/android/setup#available-libraries

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-maps:18.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
