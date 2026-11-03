# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

-optimizationpasses 5

# Keep Android Activity classes referenced in AndroidManifest
-keep class * extends android.app.Activity

# Keep JavaScriptInterface methods if any are used in WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep database model and DAO classes
-keep class inmethod.gitnotetaking.db.** { *; }

# Keep JGit TranslationBundle subclasses and fields (loaded via reflection)
-keep class * extends org.eclipse.jgit.nls.TranslationBundle {
    *;
}
-keepclassmembers enum org.eclipse.jgit.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Suppress warnings from optional desktop/server references in JGit and local JARs
-dontwarn org.eclipse.jgit.**
-dontwarn inmethod.**
-dontwarn java.lang.management.**
-dontwarn org.ietf.jgss.**
-dontwarn javax.**
-dontwarn jakarta.**
-dontwarn org.apache.**
-dontwarn org.slf4j.**

# Strip debug/verbose logs in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
