
rootProject.name = "Librera"

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

include(":app")
include(":libPro")
include(":libReflow")
include(":appCompose")
include(":appLibDrive")

include(":Builder")
include(":libDepFree")
include(":libDepPro")
