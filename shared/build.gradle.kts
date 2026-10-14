plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            api(project(":feature-notes"))
            api(project(":feature-journal"))
            api(project(":feature-tasks"))
            api(project(":feature-habits"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.okio.fakefilesystem)
        }
    }
}

android {
    namespace = "dev.dawem.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
