# R8 full mode is on. These rules cover the three things reflection touches in this app.

# 1. Room generates implementations reflectively named after the @Database class.
-keep class com.prism.studio.data.db.PrismDatabase_Impl { *; }

# 2. kotlinx.serialization generates serializers as nested classes referenced by name. WidgetSpec
#    and StyleDelta are persisted as JSON in Room, so stripping these silently breaks every placed
#    widget on upgrade — the worst possible failure mode, since it only appears in release builds.
-keepclassmembers class com.prism.studio.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.prism.studio.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.prism.studio.model.**$$serializer { *; }

# 3. AppWidgetProvider and the workers are instantiated by the system from the manifest name.
#
#    The 572 generated providers matter most here: R8 sees no code path referencing them — they are
#    only ever named as strings in the manifest — so full mode strips every one, and the widget
#    picker is empty in a release build while being full in debug. That is the worst shape of bug:
#    invisible until the build you actually ship.
-keep class com.prism.studio.widget.generated.** { *; }
-keep class com.prism.studio.widget.NativeWidgetProvider { *; }
-keep class com.prism.studio.widget.PrismWidgetProvider { *; }
-keep class com.prism.studio.widget.PrismCompactProvider { *; }
-keep class com.prism.studio.widget.PrismTallProvider { *; }
-keep class com.prism.studio.widget.PrismLargeProvider { *; }
-keep class com.prism.studio.widget.PrismBannerProvider { *; }
-keep class com.prism.studio.widget.PinResultReceiver { *; }

# The widget host launches this by name from android:configure, so nothing in our code references it.
-keep class com.prism.studio.editor.WidgetConfigureActivity { *; }
-keep class com.prism.studio.widget.BootReceiver { *; }
-keep class com.prism.studio.widget.MediaSessionListener { *; }
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# Billing responses are parsed reflectively by the library.
-keep class com.android.billingclient.** { *; }

# Keep line numbers so a Play Console stack trace is readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
