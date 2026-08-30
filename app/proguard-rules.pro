# Keep rules for release R8.

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod
-keepattributes Signature,*Annotation*

# kotlinx.serialization (backup + Drive JSON)
-keep @kotlinx.serialization.Serializable class com.errata.app.** { *; }
-keepclassmembers class com.errata.app.** {
    *** Companion;
}
-keep class com.errata.app.**$$serializer { *; }

# Room entities / DB
-keep class com.errata.app.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Receivers, WorkManager, widget (manifest-referenced)
-keep class com.errata.app.reminders.** { *; }
-keep class com.errata.app.widget.** { *; }
-keep class com.errata.app.sync.SyncWorker { *; }

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
