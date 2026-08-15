plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.prism.studio.render"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Font resources are referenced from Kotlin via R.font.
    buildFeatures { androidResources = true }
    // MonthCalendarRenderer uses java.time.temporal.WeekFields, which needs desugaring on API 26.
    compileOptions { isCoreLibraryDesugaringEnabled = true }
}
dependencies {
    api(project(":core:model"))
    // ColorUtils (Lab/HSL conversion) used by ColorHarmony.
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
}
