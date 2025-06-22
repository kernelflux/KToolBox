package com.kernelflux.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


class AndroidModuleConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.all {
            when (this) {
                is AppPlugin -> {
                    safeApplyPlugin(project, "org.jetbrains.kotlin.android")
                    safeApplyPlugin(project, "maven-publish")
                    val ext = project.extensions.getByType(ApplicationExtension::class.java)
                    configureApp(ext)
                }

                is LibraryPlugin -> {
                    safeApplyPlugin(project, "org.jetbrains.kotlin.android")
                    safeApplyPlugin(project, "maven-publish")
                    val ext = project.extensions.getByType(LibraryExtension::class.java)
                    configureLibrary(ext)
                }
            }
        }


        // 配置默认依赖
        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        project.afterEvaluate {
            project.dependencies {
                add("implementation", libs.findLibrary("androidx.core.ktx").get())
                add("implementation", libs.findLibrary("androidx.appcompat").get())
                add("implementation", libs.findLibrary("material").get())


                add("testImplementation", libs.findLibrary("junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx.junit").get())
            }
        }


        // ✅ Kotlin compilerOptions 设置
        project.plugins.withId("org.jetbrains.kotlin.android") {
            project.extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    private fun safeApplyPlugin(project: Project, pluginName: String) {
        if (!project.plugins.hasPlugin(pluginName)) {
            project.plugins.apply(pluginName)
        }
    }

    private fun configureApp(extension: ApplicationExtension) {
        extension.apply {
            compileSdk = 35

            defaultConfig {
                applicationId = "com.kernelflux.uixkitsample"
                minSdk = 21
                targetSdk = 35
                versionCode = 1
                versionName = "1.0.0"
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

            buildFeatures {
                viewBinding = true
                buildConfig = true
            }
        }
    }


    private fun configureLibrary(extension: LibraryExtension) {
        extension.apply {
            compileSdk = 35

            defaultConfig {
                minSdk = 21
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
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

            buildFeatures {
                viewBinding = true
                buildConfig = true
            }

            publishing {
                singleVariant("release") {
                    withSourcesJar()
                    withJavadocJar()
                }
            }
        }
    }

}