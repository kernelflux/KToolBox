plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.2.2") // ✅ 支持 com.android.library
    implementation(kotlin("gradle-plugin")) // ✅ 支持 org.jetbrains.kotlin.android
}

gradlePlugin {
    plugins {
        //plugin1
        val androidConfigPlugin = this.create("androidConfig")
        androidConfigPlugin.id = "com.kernelflux.android.module" // 插件 ID
        androidConfigPlugin.implementationClass =
            "com.kernelflux.android.AndroidModuleConventionPlugin" // 插件的实现类
    }
}