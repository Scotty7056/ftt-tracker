-keepclassmembers class * extends androidx.room.RoomDatabase { <init>(); }
-keepattributes *Annotation*, InnerClasses
-keep class io.ktor.** { *; }
# Room Database & DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Models & Entities
-keep class com.example.freizeittickettracker.data.** { *; }
-keepclassmembers class com.example.freizeittickettracker.data.** { *; }

# Jetpack Compose
-keepattributes *Annotation*
-dontwarn androidx.compose.**
