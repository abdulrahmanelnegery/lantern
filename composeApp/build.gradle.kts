import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.okio)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.okio.fakefilesystem)
            }
        }
    }
}

android {
    namespace = "dev.lantern.notes"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.lantern.notes"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.4.0"
    }
    signingConfigs {
        create("release") {
            val store = System.getenv("LANTERN_KEYSTORE")
            if (store != null) {
                storeFile = file(store)
                storePassword = System.getenv("LANTERN_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LANTERN_KEY_ALIAS") ?: "lantern"
                keyPassword = System.getenv("LANTERN_KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("LANTERN_KEYSTORE") != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

compose.desktop {
    application {
        mainClass = "dev.lantern.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "lantern-notes"
            packageVersion = "0.4.0"
            description = "Private notes, journal, tasks, habits and money in plain files"
            vendor = "Abdulrahman Elnegery"
            linux {
                menuGroup = "Office"
                appCategory = "Office"
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }
        }
    }
}
