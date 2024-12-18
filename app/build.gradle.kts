import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()

if (secretsFile.exists()) {
    secrets.load(secretsFile.inputStream())
} else {
    throw GradleException("secrets.properties file is missing. Please add it to the root directory.")
}

android {
    namespace = "com.example.snaplines"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.snaplines"
        minSdk = 30
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
        all {
            buildConfigField("String", "API_KEY", "\"${secrets["API_KEY"]}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation (libs.camera.core)
    implementation (libs.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)
    implementation (libs.androidx.camera.extensions)

    implementation (libs.jackson.databind)
    implementation (libs.jackson.datatype.jsr310)
    implementation (libs.retrofit)
    implementation (libs.converter.jackson)
    implementation (libs.logging.interceptor)
}