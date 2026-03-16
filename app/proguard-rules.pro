# ProGuard rules for Secure Folder

# Keep crypto classes
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# Keep Room entities
-keep class com.securefolder.data.model.** { *; }

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Compose
-dontwarn androidx.compose.**

# CameraX
-keep class androidx.camera.** { *; }
