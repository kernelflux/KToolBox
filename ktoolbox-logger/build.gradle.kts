plugins {
    alias(libs.plugins.android.library)
    id("com.kernelflux.android.module")
}

android {
    namespace = "com.kernelflux.ktoolbox.logger"
}

dependencies {
}


extra["ktoolbox.version"] = "0.0.1"