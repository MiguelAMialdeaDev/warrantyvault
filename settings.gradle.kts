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

rootProject.name = "WarrantyVault"
include(":app")
include(":core:domain")
include(":core:database")
include(":core:ui")
include(":feature:home")
include(":feature:detail")
include(":feature:addedit")
include(":feature:settings")
include(":feature:premium")
include(":feature:onboarding")
