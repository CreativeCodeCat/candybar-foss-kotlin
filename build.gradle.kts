plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}

val minSdkVal = libs.versions.minSdk.get().toInt()
val targetSdkVal = libs.versions.targetSdk.get().toInt()
val compileSdkVal = libs.versions.compileSdk.get().toInt()

allprojects {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.google.com") }
    }

    val major = 3
    val minor = 22
    val patch = 2

    extra.set("VersionCode", major * 10000 + minor * 100 + patch)
    extra.set("VersionName", "$major.$minor.$patch")

    extra.set("MinSdk", minSdkVal)
    extra.set("TargetSdk", targetSdkVal)
    extra.set("CompileSdk", compileSdkVal)
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
