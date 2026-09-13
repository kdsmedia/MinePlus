# Keep JNI entry points so the native X11 engine stays linked.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the native library loader.
-keep class com.altomedia.mineplus.crypto.X11Engine { *; }