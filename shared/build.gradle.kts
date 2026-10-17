import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js {
        nodejs()
    }

    linuxX64()

    sourceSets {
        commonMain.dependencies {
            // Intentionally empty: the parsing/resolver seam is pure Kotlin.
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.test.coroutines)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.core)
        }
        linuxX64Main.dependencies {
            implementation(libs.ktor.client.curl)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
