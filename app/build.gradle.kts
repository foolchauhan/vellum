plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  id("com.google.gms.google-services")
}

android {
    namespace = "com.example.vellum"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.vellum"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.forEach { output ->
            val apkOutput = output as? com.android.build.gradle.internal.api.ApkVariantOutputImpl
            if (apkOutput != null && variant.buildType.name == "release") {
                apkOutput.outputFileName = "Vellum.apk"
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
  implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room
  implementation(libs.androidx.room.runtime)
  add("kapt", libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)

  // Google Fonts
  implementation(libs.androidx.compose.ui.text.google.fonts)

  // Google Play Services Auth
  implementation(libs.google.play.services.auth)

  implementation("com.google.firebase:firebase-analytics")

  // Coil Compose for image loading
  implementation("io.coil-kt:coil-compose:2.6.0")
}

tasks.register("copyReleaseApkToRoot") {
    val srcFile = layout.buildDirectory.file("outputs/apk/release/Vellum.apk")
    val destFile = layout.projectDirectory.file("../Vellum.apk")

    inputs.file(srcFile).withPropertyName("sourceApk")
    outputs.file(destFile).withPropertyName("destinationApk")

    doLast {
        val src = srcFile.get().asFile
        val dest = destFile.asFile
        if (src.exists()) {
            src.copyTo(dest, overwrite = true)
            println("Successfully copied Vellum.apk to project root: ${dest.absolutePath}")
        } else {
            println("Warning: Release APK not found at ${src.absolutePath}")
        }
    }
}

afterEvaluate {
    tasks.named("assembleRelease") {
        finalizedBy("copyReleaseApkToRoot")
    }
}
