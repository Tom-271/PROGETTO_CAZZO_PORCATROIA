pluginManagement {
    plugins {
        id("com.google.devtools.ksp") version "2.0.21-1.0.26"
    }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    // Qui dichiariamo i repository che TUTTI i moduli useranno
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Repository DSI ANT+
        maven {
            url = uri("https://dl.bintray.com/dsi-antplus/maven")
        }
    }
}

rootProject.name = "PROGETTO_TOSA"
include(":app")
