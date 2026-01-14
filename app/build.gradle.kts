import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    signingConfigs {
        create("release") {
            keyAlias = "key0"
            keyPassword = "candybar"
            storeFile = file("candybar.jks")
            storePassword = "candybar"

            enableV1Signing = true
            enableV2Signing = true
        }
    }

    compileSdk = libs.versions.compileSdk.get().toInt()

    namespace = "com.candybar.dev"

    defaultConfig {
        applicationId = "com.candybar.dev"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = (rootProject.extra.get("VersionCode") as Int) * 10 + 0
        versionName = rootProject.extra.get("VersionName") as String

        // This code loads the license key from `local.properties` file
        // and saves it as build config field named "LICENSE_KEY"
        var licenseKey = ""
        val props = Properties()
        val propFile = rootProject.file("local.properties")
        if (propFile.exists()) {
            propFile.inputStream().use { props.load(it) }
            licenseKey = props.getProperty("license_key", "")
        }
        buildConfigField("String", "LICENSE_KEY", "\"$licenseKey\"")
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }


    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":library"))

    implementation(libs.kotlin.stdlib)

    // TODO: Remove `//` below to enable OneSignal
    //implementation("com.onesignal:OneSignal:[5.0.0, 5.99.99]")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
            useVersion(libs.versions.kotlin.get())
        }
    }
}

