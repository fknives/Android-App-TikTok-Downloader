import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
}

val signingProperties = Properties()
val signingFile = rootProject.file("androidApp/signing.config.properties")

if (signingFile.exists()) {
    signingFile.inputStream().use(signingProperties::load)
} else {
    println("Warning: No Signing Config found")
}

val releaseKeyAlias: String = signingProperties.getProperty("KEY_ALIAS", "")
val releaseKeyPassword: String = signingProperties.getProperty("KEY_PASSWORD", "")
val releaseStorePassword: String = signingProperties.getProperty("STORE_PASSWORD", "")
val releaseKeystoreFile: String = signingProperties.getProperty("KEYSTORE_FILE", "")

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "org.fnives.tiktokdownloader"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.fnives.tiktokdownloader"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 6
        versionName = "1.3.5"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
            if (releaseKeystoreFile.isNotBlank()) {
                storeFile = file(releaseKeystoreFile)
            }
            storePassword = releaseStorePassword
        }
    }
    buildTypes {
        debug {
            versionNameSuffix = "-dev"
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isShrinkResources = false
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    testOptions.unitTests {
        isIncludeAndroidResources = true
        isReturnDefaultValues = true
        all { test ->
            test.useJUnitPlatform()
            test.testLogging {
                events("started", "passed", "skipped", "failed")
                exceptionFormat = TestExceptionFormat.FULL
                showStandardStreams = true
            }
        }
    }
    lint {
        abortOnError = true
        disable.add("NullSafeMutableLiveData")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)

    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.gson)

    implementation(libs.youtube)
    implementation(libs.io.coil.compose)
    implementation(libs.io.coil.network)
    implementation(libs.io.coil.video)
    implementation(libs.compose.reorderable)

    testImplementation(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockito)
    testRuntimeOnly(libs.junit5.platform.launcher)
    testImplementation(libs.okhttp.mock.webserver)
    testImplementation(libs.test.io)
    testImplementation(libs.kotlinx.test.coroutines)
    testImplementation(libs.kotlin.test.turbine)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}