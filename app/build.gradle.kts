plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.celnet.syncro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.celnet.syncro"
        minSdk = 30
        targetSdk = 36
        versionCode = 15
        versionName = "3.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    buildFeatures {
        buildConfig = true
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    //implementation(libs.room.common.jvm)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.gurux:gurux.dlms:4.0.79")
    implementation("org.gurux:gurux.common:1.0.17")
    implementation("org.gurux:gurux.serial:1.0.29")
    implementation("org.gurux:gurux.net:1.0.30")
    implementation("com.github.mwiede:jsch:2.28.8")
    implementation("commons-net:commons-net:3.9.0")
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("com.fasterxml.woodstox:woodstox-core:6.2.8")
    implementation("androidx.room:room-runtime:2.8.5")
    annotationProcessor("androidx.room:room-compiler:2.8.5")
// Room 2.8.x exige minSdk 23. Con minSdk 21/22 usa la serie 2.7.x.
// Room 2.6.1 falla con proyectos compilados con Kotlin 2.1:
// "Provided Metadata instance has version 2.1.0, while maximum supported version is 2.0.0".
    implementation("androidx.recyclerview:recyclerview:1.3.2")   // si no la tienes ya
}