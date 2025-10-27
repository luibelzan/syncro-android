plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.syncro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.syncro"
        minSdk = 30
        targetSdk = 36
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.gurux:gurux.dlms:4.0.79")
    implementation("org.gurux:gurux.common:1.0.17")
    implementation("org.gurux:gurux.serial:1.0.29")
    implementation("org.gurux:gurux.net:1.0.30")

}