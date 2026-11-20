# Built by MD.Nirob
# ProGuard / R8 Obfuscation & Shrinking Rules for Https_Browser (com.rootdeck.https_browser)

# Preserve line numbers for debugging and stack traces
-keepattributes SourceFile,LineNumberTable,InnerClasses,EnclosingMethod
-keepattributes *Annotation*,Signature,Exceptions

# Keep JavaScript Interface methods called from WebView JavaScript (Task 203)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep JavaScript Interface bridges
-keep class com.rootdeck.https_browser.engine.ConsoleInterceptor { *; }
-keep class com.rootdeck.https_browser.engine.InspectElementMode { *; }

# Keep Room Database, DAOs, and Entities (Task 202)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.TypeConverter class *
-keep class com.rootdeck.https_browser.data.** { *; }
-dontwarn androidx.room.paging.**

# Keep Pagefile state models and serialization (Task 204)
-keep class com.rootdeck.https_browser.pagefile.** { *; }
-keep class com.rootdeck.https_browser.pagefile.TabState { *; }
-keep class com.rootdeck.https_browser.pagefile.TabPagefileManager { *; }
-keep class com.rootdeck.https_browser.engine.EngineConfig { *; }
-keep class com.rootdeck.https_browser.engine.DomNode { *; }
-keep class com.rootdeck.https_browser.engine.NetworkRequestItem { *; }
-keep class com.rootdeck.https_browser.devtools.NetworkEntry { *; }
-keep class com.rootdeck.https_browser.engine.ConsoleMessageItem { *; }

# Reader mode bridge keep (Task 205)
-keep class com.rootdeck.https_browser.reader.ReaderModeExtractor { *; }
-keep class com.rootdeck.https_browser.reader.** { *; }

# Feature models keep (Task 206)
-keep class com.rootdeck.https_browser.readinglist.** { *; }
-keep class com.rootdeck.https_browser.readinglist.ReadingListEntity { *; }
-keep class com.rootdeck.https_browser.session.** { *; }
-keep class com.rootdeck.https_browser.session.SessionEntity { *; }
-keep class com.rootdeck.https_browser.session.SessionTabEntity { *; }
-keep class com.rootdeck.https_browser.pip.** { *; }
-keep class com.rootdeck.https_browser.capture.** { *; }

# Compose keep (Task 207)
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Chromium WebKit classes & clients (Task 208)
-keepclassmembers class * extends android.webkit.WebViewClient { public *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { public *; }
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# PDF Viewer
-keep class com.rootdeck.https_browser.pdf.** { *; }

# Jetpack Compose & Kotlin rules
-dontwarn org.jetbrains.annotations.**
-dontwarn java.lang.invoke.**
