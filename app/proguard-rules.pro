# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Gson / settings models
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.ambrxsh.buzzbuddy.model.** { *; }
-keep class com.ambrxsh.buzzbuddy.dtos.** { *; }
-keep class com.ambrxsh.buzzbuddy.clients.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

# Keep BroadcastReceivers, Services, Activities referenced from the manifest
-keep class com.ambrxsh.buzzbuddy.BuzzBuddyApp { *; }
-keep class com.ambrxsh.buzzbuddy.AlarmReceiver { *; }
-keep class com.ambrxsh.buzzbuddy.BootReceiver { *; }
-keep class com.ambrxsh.buzzbuddy.AlarmActivity { *; }
-keep class com.ambrxsh.buzzbuddy.services.BuzzBuddyAlarmForegroundService { *; }

# Coroutines (Room / ViewModel)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
