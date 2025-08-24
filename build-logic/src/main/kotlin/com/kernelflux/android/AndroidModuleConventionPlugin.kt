package com.kernelflux.android

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.optionals.getOrNull


class AndroidModuleConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.configureAndroidModule<LibraryExtension>("com.android.library") { ext ->
            configureLibrary(ext, project)
        }
        project.configureAndroidModule<ApplicationExtension>("com.android.application") { ext ->
            configureApp(ext, project)
            copyAndRenameApk(project)
        }

        // 配置默认依赖
        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        project.afterEvaluate {
            project.dependencies {
                libs.findLibrary("androidx.core.ktx").getOrNull()?.let {
                    add("implementation", it)
                }
                libs.findLibrary("androidx.appcompat").getOrNull()?.let {
                    add("implementation", it)
                }
                libs.findLibrary("material").getOrNull()?.let {
                    add("implementation", it)
                }

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

    private inline fun <reified T> Project.configureAndroidModule(
        pluginId: String,
        crossinline configure: (T) -> Unit,
    ) {
        plugins.withId(pluginId) {
            safeApplyPlugin(this@configureAndroidModule, "org.jetbrains.kotlin.android")
            safeApplyPlugin(this@configureAndroidModule, "maven-publish")
            extensions.findByType(T::class.java)?.let { ext ->
                configure(ext)
            }
        }
    }

    @Suppress("SimpleDateFormat")
    private fun copyAndRenameApk(project: Project) {
        val androidComponents =
            project.extensions.getByType<ApplicationAndroidComponentsExtension>()
        androidComponents.onVariants { variant ->
            val versionName = variant.outputs.firstOrNull()?.versionName?.orNull ?: "9.9.9"
            val copyApkTask =
                project.tasks.register<Copy>("export${variant.name.replaceFirstChar { it.uppercaseChar() }}Apk") {
                    group = "commonApkOp"
                    description = "Export ${variant.name} APK to root apks folder"

                    from(variant.artifacts.get(SingleArtifact.APK)) {
                        rename { _ ->
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                            "${variant.name}-${versionName}-${timeStamp}.apk"
                        }
                    }
                    into(project.rootProject.layout.projectDirectory.dir("apks"))

                    // 处理重复文件的策略: 保留重复文件，Gradle 会重命名
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }

            // 让 assembleXXX 完成后执行 copy 任务
            project.tasks.named("build").configure {
                finalizedBy(copyApkTask)
            }
        }
    }

    @Suppress("SimpleDateFormat")
    private fun configureApp(extension: ApplicationExtension, project: Project) {
        extension.apply {
            compileSdk = 35

            defaultConfig {
                applicationId = "com.kernelflux.ktoolboxsample"
                minSdk = 21
                targetSdk = 35
                versionCode = 1
                versionName = "1.0.0"
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                    proguardFiles(project.file("proguard-rules.pro"))
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

    private fun configureLibrary(extension: LibraryExtension, project: Project) {
        extension.apply {
            compileSdk = 35

            defaultConfig {
                minSdk = 21
                consumerProguardFiles(project.file("consumer-rules.pro"))
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                    proguardFiles(project.file("proguard-rules.pro"))
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
                }
            }
        }
    }

}