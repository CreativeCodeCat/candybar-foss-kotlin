/*
 * CandyBar - Material Dashboard
 *
 * Copyright (c) 2014-2016 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.donnnno.candybar"
            artifactId = "library"
            version = rootProject.extra.get("VersionName") as String

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        // options.compilerArgs += listOf("-Xlint:unchecked", "-Xlint:deprecation")
    }
}

android {
    compileSdk = rootProject.extra.get("CompileSdk") as Int

    namespace = "candybar.lib"

    defaultConfig {
        minSdk = rootProject.extra.get("MinSdk") as Int
        targetSdk = rootProject.extra.get("TargetSdk") as Int
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "VERSION_NAME", "\"${rootProject.extra.get("VersionName")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    packaging {
        resources {
            excludes += listOf("META-INF/NOTICE", "META-INF/LICENSE")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    lint {
        abortOnError = false
        disable += "MissingTranslation"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion("2.0.21")
            }
        }
    }
}

dependencies {
    implementation(project(":extLibs:PreLollipopTransitions"))

    api(libs.androidx.annotation)
    api(libs.androidx.appcompat)
    api(libs.androidx.multidex)
    api(libs.muzei.api)
    api(libs.kustom.api)
    api(libs.kustom.preset)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.palette)
    implementation(libs.material)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.logansquare)
    annotationProcessor(libs.logansquare.compiler)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    implementation(libs.okhttp)
    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.commons)
    implementation(libs.circularimageview)
    implementation(libs.photoview)
    implementation(libs.autofittextview)
    implementation(libs.taptargetview)
    implementation(libs.adaptive.icon.bitmap)
    implementation(libs.recycler.fast.scroll)
    implementation(libs.fastscroll)

    implementation(libs.cafebar)
    implementation(libs.android.helpers.core)
    implementation(libs.android.helpers.animation)
    implementation(libs.android.helpers.permission)
    implementation(libs.drawme)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
