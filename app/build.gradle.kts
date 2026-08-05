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
        versionCode = 9
        versionName = "1.0.8"

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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.gurux:gurux.dlms:4.0.79")
    implementation("org.gurux:gurux.common:1.0.17")
    implementation("org.gurux:gurux.serial:1.0.29")
    implementation("org.gurux:gurux.net:1.0.30")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("commons-net:commons-net:3.9.0")
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("com.fasterxml.woodstox:woodstox-core:6.2.8")

}