# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
# Type converters
-keep class com.miguelcode.warrantyvault.database.converter.** { *; }

# ============================================================
# Koin
# ============================================================
-keep class org.koin.** { *; }
-keep class * extends org.koin.core.module.Module { *; }
# KoinComponent implementations (e.g. WarrantyExpirationWorker)
-keep class * implements org.koin.core.component.KoinComponent { *; }

# ============================================================
# WorkManager
# ============================================================
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============================================================
# Google Play Billing 6.x
# ============================================================
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ============================================================
# DataStore (Preferences, no Proto en este proyecto)
# ============================================================
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ============================================================
# Domain models — usados por Compose state y mappers
# ============================================================
-keep class com.miguelcode.warrantyvault.domain.model.** { *; }

# ============================================================
# Compose — R8 ya incluye reglas por defecto, esto silencia warnings
# ============================================================
-dontwarn androidx.compose.**
