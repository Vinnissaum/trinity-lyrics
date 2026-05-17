pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "trinity-lyrics"

include(":app")
include(":core:domain")
include(":core:db")
include(":core:ui")
include(":feature:lyrics")
include(":feature:presentation")
include(":feature:media")
include(":feature:import")
