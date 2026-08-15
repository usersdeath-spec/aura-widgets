plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.prism.studio.editor"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // WidgetConfigureActivity uses LocalDateTime.
    // AGP needs BOTH the flag and the dependency below; either one alone silently does nothing.
    compileOptions { isCoreLibraryDesugaringEnabled = true }
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:design"))
    implementation(project(":core:render"))
    implementation(project(":core:data"))
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
}
