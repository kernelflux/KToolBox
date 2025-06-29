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

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
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

