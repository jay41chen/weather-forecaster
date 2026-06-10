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

rootProject.name = "weather forecaster"

include(":app")
include(":core")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":feature:weather")
include(":feature:citylist")
include(":demo")
