# Project-specific R8/ProGuard rules.
#
# AndroidX, Room, DataStore, and Compose ship their own consumer rules; no
# custom keep rules are needed beyond stripping debug logging from release.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}
