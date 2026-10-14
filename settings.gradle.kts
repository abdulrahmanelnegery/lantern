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
rootProject.name = "dawem"
include(":core", ":ui", ":feature-notes", ":feature-journal", ":feature-tasks", ":feature-habits", ":shared", ":composeApp")
