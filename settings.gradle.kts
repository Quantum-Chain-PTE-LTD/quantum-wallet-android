pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Quantum Wallet"

// Quantum Kit — composite build for local development
// Switch to published Maven artifacts for release builds by removing this block
// and using the version catalog coordinates (libs.kit.quantum, libs.kit.qrc20)
includeBuild("../quantum-kit-android") {
    dependencySubstitution {
        substitute(module("com.quantum:quantumkit")).using(project(":quantumkit"))
        substitute(module("com.quantum:qrc20kit")).using(project(":qrc20kit"))
    }
}

// Quantum Market Kit — composite build for local development
includeBuild("../market-kit-android") {
    dependencySubstitution {
        substitute(module("com.github.horizontalsystems:market-kit-android")).using(project(":marketkit"))
    }
}

include(":app")
include(":core")
include(":components:icons")
include(":components:chartview")
