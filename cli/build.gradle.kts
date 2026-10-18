plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    linuxX64 {
        binaries.executable {
            entryPoint = "org.fnives.tiktokdownloader.cli.main"
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
