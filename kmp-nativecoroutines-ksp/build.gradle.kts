plugins {
    id("kmp-nativecoroutines-kotlin-jvm")
    id("kmp-nativecoroutines-publish")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinCompileTesting.ksp)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":kmp-nativecoroutines-annotations"))
}

kotlin {
    explicitApi()
    jvmToolchain(11)
}

tasks.compileKotlin.configure {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}
