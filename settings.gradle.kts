import java.util.Properties
import kotlin.apply

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}


val localProps = Properties().apply {
    val file = File(rootDir, "private.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val gprUser = localProps.getProperty("gpr.user") ?: System.getenv("GPR_USER")
val gprKey = localProps.getProperty("gpr.key") ?: System.getenv("GPR_KEY")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://maven.pkg.github.com/kernelflux/KToolBox")
            credentials {
                username = gprUser
                password = gprKey
            }
        }


    }
}

rootProject.name = "ktoolbox"
include(":app")
include(":ktoolbox-bundle")

include(":ktoolbox-core")
include(":ktoolbox-display")
include(":ktoolbox-device")
include(":ktoolbox-media")
include(":ktoolbox-network")
include(":ktoolbox-parser")
include(":ktoolbox-storage")
include(":ktoolbox-string")
include(":ktoolbox-system")
include(":ktoolbox-thread")
include(":ktoolbox-logger")
