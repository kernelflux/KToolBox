// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

subprojects {
    // 自动引入发布脚本
    apply(from = "${rootDir}/gradle/publish.gradle.kts")
}

tasks.register("publishAllModules") {
    dependsOn(
        ":ktoolbox-bundle:publish",
        ":ktoolbox-core:publish",
        ":ktoolbox-device:publish",
        ":ktoolbox-media:publish",
        ":ktoolbox-network:publish",
        ":ktoolbox-parser:publish",
        ":ktoolbox-storage:publish",
        ":ktoolbox-string:publish",
        ":ktoolbox-system:publish",
        ":ktoolbox-thread:publish"
    )
}