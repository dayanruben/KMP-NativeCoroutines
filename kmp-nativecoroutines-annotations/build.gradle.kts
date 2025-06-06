import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kmp-nativecoroutines-kotlin-multiplatform")
    id("kmp-nativecoroutines-publish")
}

kotlin {
    explicitApi()
    jvmToolchain(11)

    macosX64()
    macosArm64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    watchosDeviceArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    jvm()
    js {
        browser()
        nodejs()
    }
    linuxArm64()
    linuxX64()
    mingwX64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        d8()
    }
}
