pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FocusLauncher"
include(":app")
include(":core:database")
include(":core:model")
include(":core:data")
include(":core:common")
include(":feature:launcher")
include(":feature:settings")
