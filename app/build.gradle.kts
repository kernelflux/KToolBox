plugins {
    alias(libs.plugins.android.application)
    id("com.kernelflux.android.module")
}

android {
    namespace = "com.kernelflux.ktoolboxsample"
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(project(":ktoolbox-logger"))
}