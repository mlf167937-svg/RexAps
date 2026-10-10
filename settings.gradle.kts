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
        // Tambahkan repository Mozilla di sini
        maven {
            url 'https://maven.mozilla.org/maven2/'
        }
    }
}

rootProject.name = "RexAps"
include(":app")