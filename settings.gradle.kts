// -------------------------------------------------------------------------------------------------
// SDK auto-detection
// -------------------------------------------------------------------------------------------------
// `local.properties` holds the absolute path to the Android SDK. It is machine-specific and
// gitignored, so every freshly extracted copy of this project has none — and Gradle then fails with
// "SDK location not found" before compiling a single line. That is a confusing first impression of
// an otherwise healthy project, and it has cost real build cycles here.
//
// So: find the SDK and write the file, before the Android plugin looks for it. This runs at
// configuration time in settings.gradle.kts, which is the only place early enough.
//
// It only ever writes when the file is absent, so a hand-edited path is never overwritten. If no
// SDK is found it stays silent and lets the plugin report its own (accurate) error.
run {
    val localProperties = File(rootDir, "local.properties")
    if (!localProperties.exists()) {
        val candidates = listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            System.getProperty("user.home") + "/Android/Sdk",
            System.getProperty("user.home") + "/Library/Android/sdk",
            System.getProperty("user.home") + "/AppData/Local/Android/Sdk",
        )
        val sdk = candidates.map(::File).firstOrNull { File(it, "platform-tools").isDirectory }
        if (sdk != null) {
            // Written as a Java properties value, where a backslash is an escape character — an
            // unescaped Windows path silently resolves to the wrong directory.
            val escaped = sdk.absolutePath.replace("\\", "\\\\")
            localProperties.writeText("# Written automatically by settings.gradle.kts. Machine-specific; not committed.\nsdk.dir=$escaped\n")
            println("Android SDK detected at $sdk — wrote local.properties")
        }
    }
}

pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "Prism"

include(":app")

// Core: pure logic, no feature knowledge. Everything below depends upward only.
include(":core:model")     // Immutable domain types. Zero Android deps except graphics primitives.
include(":core:design")    // App-side design system (Compose M3 theme, type scale, tokens).
include(":core:render")    // Canvas rendering engine shared by widgets AND in-app previews.
include(":core:data")      // Room, DataStore, repositories, license state.

// Features: UI surfaces. Never depend on each other.
include(":feature:catalog")
include(":feature:editor")
include(":feature:wallpapers")
include(":feature:onboarding")
include(":feature:settings")

// Widget host: RemoteViews providers, receivers, update scheduling.
include(":widget")
