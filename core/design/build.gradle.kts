plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "com.prism.studio.design"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    // WidgetPreview draws a real widget bitmap, so the design system needs the renderer.
    // api, not implementation: callers receive PrismRenderer and WidgetData in its signature.
    api(project(":core:render"))

    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)
    api(libs.compose.material.icons)
    api(libs.compose.animation)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
}
