# The player page calls these by name through the JavaScript bridge.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Crash and problem reports keep line numbers, so the release's mapping.txt turns them back into source names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
