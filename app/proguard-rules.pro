# ProGuard rules for SonarFiles

# Keep Hilt / Dagger generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao class * { *; }
-keep class * implements androidx.room.RoomDatabase { *; }

# Keep domain models serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# SMBJ & SSHJ & BouncyCastle rules
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }
-dontwarn com.hierynomus.**
-keep class com.hierynomus.** { *; }
-dontwarn net.schmizz.**
-keep class net.schmizz.** { *; }
