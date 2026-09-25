pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        // API Bukkit/Spigot : le dépôt public de SpigotMC (inclut les dépendances de l'API).
        maven("https://hub.spigotmc.org/nexus/content/groups/public/") {
            name = "spigotmc"
        }
        // VaultAPI est publié via JitPack ; on limite ce dépôt à ce seul groupe.
        maven("https://jitpack.io") {
            name = "jitpack"
            content { includeGroup("com.github.MilkBowl") }
        }
    }
}

rootProject.name = "EGC-plugins"

include(":libs:common")
include(":libs:compat")
include(":libs:nms")
include(":plugins:ultimate-core")
