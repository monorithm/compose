pluginManagement {
    // The build logic of the language: the emitter of the tokens and the check of the IR.
    includeBuild("build-logic")
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

rootProject.name = "compose"
include(":ui")
