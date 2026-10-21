# ProGuard & R8 rules for SonarFiles

# Keep Hilt / Dagger generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao class * { *; }
-keep class * implements androidx.room.RoomDatabase { *; }

# Keep domain models serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# SMBJ, MBassador & javax.el
-dontwarn javax.el.**
-dontwarn net.engio.mbassy.**
-keep class net.engio.mbassy.** { *; }
-dontwarn com.hierynomus.**
-keep class com.hierynomus.** { *; }

# SSHJ & EdDSA
-dontwarn net.schmizz.**
-keep class net.schmizz.** { *; }
-dontwarn net.i2p.crypto.eddsa.**
-keep class net.i2p.crypto.eddsa.** { *; }
-dontwarn sun.security.**

# BouncyCastle
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }

# JmDNS (mDNS Discovery)
-dontwarn javax.jmdns.**
-keep class javax.jmdns.** { *; }

# Logging (SLF4J)
-dontwarn org.slf4j.**
