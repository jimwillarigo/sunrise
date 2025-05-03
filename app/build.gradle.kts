plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Add the Google services plugin
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
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

    buildFeatures {
        mlModelBinding = true
        viewBinding = true
    }
}

dependencies {
    // App dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("androidx.appcompat:appcompat:1.3.1")

    // TensorFlow dependencies
    implementation("org.tensorflow:tensorflow-lite-support:0.1.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.1.0-rc1")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0-rc1")

    // Firebase dependencies
    implementation("com.google.firebase:firebase-auth:22.0.0") // Firebase Authentication
    implementation("com.google.firebase:firebase-database:21.0.0") // Firebase Realtime Database
    implementation("com.google.firebase:firebase-storage:20.2.0") // Firebase Storage
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3") // Glide for image loading
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0") // Glide annotation processor

    implementation ("androidx.recyclerview:recyclerview:1.3.1")


    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// Google services classpath
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath ("com.google.gms:google-services:4.3.10") // Check for the latest version
    }
}
