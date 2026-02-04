# ProGuard rules for Grevling
# https://www.guardsquare.com/manual/configuration/usage

# ============================================================
# GENERAL ANDROID RULES
# ============================================================

# Keep Android components (Activities, Services, Receivers, etc.)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.appwidget.AppWidgetProvider

# Keep Fragment classes
-keep public class * extends androidx.fragment.app.Fragment

# ============================================================
# JAVAMAIL (SMTP)
# ============================================================

# JavaMail krever at disse klassene beholdes for reflection
-dontwarn java.awt.**
-dontwarn javax.security.**
-dontwarn javax.activation.**

-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.mail.** { *; }
-keep class com.sun.activation.** { *; }

# Mailcap og MIME handlers
-keep class javax.mail.internet.** { *; }
-keepclassmembers class javax.mail.internet.** { *; }

# ============================================================
# ENCRYPTED SHARED PREFERENCES
# ============================================================

# AndroidX Security (bruker reflection)
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# ============================================================
# APP-SPECIFIC RULES
# ============================================================

# Keep PreferencesManager (brukes av widgets via reflection-lignende oppslag)
-keep class com.smsforwarder.PreferencesManager { *; }

# Keep EmailSender
-keep class com.smsforwarder.EmailSender { *; }

# Keep Logger (bruker BuildConfig reflection)
-keep class com.smsforwarder.Logger { *; }

# Keep data classes
-keep class com.smsforwarder.AppInfo { *; }

# ============================================================
# DEBUGGING (fjern i produksjon hvis ønskelig)
# ============================================================

# Behold linje-numre for stack traces (nyttig for debugging)
-keepattributes SourceFile,LineNumberTable

# Skjul original filnavn (ekstra sikkerhet)
-renamesourcefileattribute SourceFile

# ============================================================
# KOTLIN
# ============================================================

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ============================================================
# SUPPRESS WARNINGS
# ============================================================

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Google Tink (brukt av EncryptedSharedPreferences)
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn javax.annotation.concurrent.ThreadSafe
-dontwarn org.joda.time.Instant
