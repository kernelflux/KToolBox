plugins {
    alias(libs.plugins.android.application)
    id("com.kernelflux.android.module")
}

android {
    namespace = "com.kernelflux.ktoolboxsample"

    signingConfigs {
        create("release") {
            storeFile = file("sample.jks")
            storePassword = "Kt3664156"
            keyAlias = "kt"
            keyPassword = "Kt3664156"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

}


dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    debugImplementation(project(":ktoolbox-logger"))
    debugImplementation(project(":ktoolbox-thread"))
    releaseImplementation("com.kernelflux.ktoolbox:logger:0.0.3")
    releaseImplementation("com.kernelflux.ktoolbox:thread:0.0.3")

}
