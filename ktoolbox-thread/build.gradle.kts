plugins {
    alias(libs.plugins.android.library)
    id("com.kernelflux.android.module")
}

android {
    namespace = "com.kernelflux.ktoolbox.thread"
}

dependencies {
    //noinspection UseTomlInstead
    releaseApi("com.kernelflux.ktoolbox:logger:0.0.3")
    debugApi(project(":ktoolbox-logger"))
}

extra["ktoolbox.version"] = "0.0.3"