plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        nodejs()
        useEsModules()
        binaries.library()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":shared"))
        }
    }
}
