import java.io.File

pluginManagement {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Movies_App"
include(":app")

// ---------------------------------------------------------------------------
// Projet sous OneDrive : Gradle écrit les sorties dans %LOCALAPPDATA% pour
// éviter les erreurs « Unable to delete directory » (verrouillage OneDrive).
// ---------------------------------------------------------------------------
val projectDirPath = settingsDir.invariantSeparatorsPath
if (projectDirPath.contains("OneDrive", ignoreCase = true)) {
    val externalBuildRoot = File(
        System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
        "AndroidBuild/Movies_App-android"
    )
    externalBuildRoot.mkdirs()
    gradle.projectsLoaded {
        gradle.rootProject.allprojects.forEach { project ->
            val moduleBuildDir = if (project == project.rootProject) {
                File(externalBuildRoot, "root")
            } else {
                File(externalBuildRoot, project.name)
            }
            moduleBuildDir.mkdirs()
            project.layout.buildDirectory.set(moduleBuildDir)
        }
    }
}
