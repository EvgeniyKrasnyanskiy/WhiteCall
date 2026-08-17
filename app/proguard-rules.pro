# WhiteCall Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * extends androidx.room.RoomDatabase

-keep class com.whitecall.app.data.local.entity.** { *; }
-keep class com.whitecall.app.domain.model.** { *; }
-keep class com.whitecall.app.util.UpdateInfo { *; }
