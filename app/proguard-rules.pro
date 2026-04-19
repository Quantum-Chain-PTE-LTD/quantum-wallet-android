# ============================================================
# Debugging: preserve line numbers in stack traces
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# Kotlin metadata / annotations
# ============================================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses, EnclosingMethod

# ============================================================
# Enums — must keep values() and valueOf() for reflection
# ============================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================
# Parcelable
# ============================================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================================
# Serializable
# ============================================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# Gson — keep fields annotated with @SerializedName
# ============================================================
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================
# Retrofit — keep annotated interface methods
# ============================================================
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn kotlin.Unit

# ============================================================
# OkHttp
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ============================================================
# Room — keep entities, DAOs, and database classes
# ============================================================
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers @androidx.room.TypeConverter class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# ============================================================
# WorkManager workers
# ============================================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============================================================
# App: entities and storage — used by Room and Gson
# ============================================================
-keep class com.quantum.wallet.bankwallet.entities.** { *; }
-keep class com.quantum.wallet.bankwallet.core.storage.** { *; }

# ============================================================
# App: backup data classes — deserialized from JSON via Gson
# ============================================================
-keep class com.quantum.wallet.bankwallet.modules.backuplocal.BackupLocalModule { *; }
-keep class com.quantum.wallet.bankwallet.modules.backuplocal.BackupLocalModule$** { *; }
-keep class com.quantum.wallet.bankwallet.modules.backuplocal.fullbackup.BackupProvider { *; }
-keep class com.quantum.wallet.bankwallet.modules.backuplocal.fullbackup.BackupProvider$** { *; }

# ============================================================
# App: EVM label provider — JSON API response models
# ============================================================
-keep class com.quantum.wallet.bankwallet.core.providers.EvmLabelProvider$** { *; }

# ============================================================
# App: pin storage (Room entity with encrypted fields)
# ============================================================
-keep class com.quantum.wallet.bankwallet.modules.pin.core.** { *; }

# ============================================================
# App: pro features storage
# ============================================================
-keep class com.quantum.wallet.bankwallet.modules.profeatures.storage.** { *; }

# ============================================================
# App: chart indicator settings (Room entity)
# ============================================================
-keep class com.quantum.wallet.bankwallet.modules.chart.ChartIndicatorSetting { *; }
-keep class com.quantum.wallet.bankwallet.modules.chart.ChartIndicatorSettingsDao { *; }

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================
# RxJava 2
# ============================================================
-dontwarn rx.**
-dontwarn io.reactivex.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# ============================================================
# Web3 — suppress warnings from optional deps
# ============================================================
-dontwarn com.sun.jna.**
-dontwarn org.slf4j.**
-dontwarn javax.naming.**
-dontwarn sun.security.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.SignalHandler
-dontwarn android.support.**

# ============================================================
# Bouncy Castle (used by crypto kits)
# ============================================================
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ============================================================
# Tor — keep Tor-related classes accessed by name
# ============================================================
-keep class net.freehaven.tor.control.** { *; }
-dontwarn net.freehaven.tor.control.**

# ============================================================
# Suppress common missing-class warnings from transitive deps
# ============================================================
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.checkerframework.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ============================================================
# Lombok — used by Stellar SDK at compile time only; not in runtime classpath
# ============================================================
-dontwarn lombok.Generated
-dontwarn lombok.NonNull


