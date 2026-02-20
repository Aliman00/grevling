# ============================================================================
# PROGUARD RULES - GREVLING APPEN
# ============================================================================
# Disse reglene forteller R8/ProGuard hvilke klasser som IKKE skal optimaliseres
# eller obfuskeres (omnavnes). Dette er kritisk for klasser som bruker reflection.

# ----------------------------------------------------------------------------
# ANDROID COMPONENTS - Behold alle Activities, Services, Receivers, Widgets
# ----------------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.appwidget.AppWidgetProvider


# ----------------------------------------------------------------------------
# JAVAMAIL - Kritisk for e-post funksjonalitet
# ----------------------------------------------------------------------------
# JavaMail bruker reflection for å laste klasser dynamisk
# Hvis disse fjernes, vil e-post-sending feile i release-bygg

# Ignorer warnings for manglende Java AWT/Security klasser
# (disse finnes ikke på Android, men JavaMail refererer til dem)
-dontwarn java.awt.**
-dontwarn javax.security.**
-dontwarn javax.activation.**

# Behold alle JavaMail klasser
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.mail.** { *; }
-keep class com.sun.activation.** { *; }

# Behold alle members i JavaMail internet-klasser
-keep class javax.mail.internet.** { *; }
-keepclassmembers class javax.mail.internet.** { *; }

# ----------------------------------------------------------------------------
# ANDROIDX SECURITY CRYPTO - For EncryptedSharedPreferences
# ----------------------------------------------------------------------------
# Security-crypto bruker reflection for kryptering
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# ----------------------------------------------------------------------------
# APP-SPESIFIKKE KLASSER
# ----------------------------------------------------------------------------
# PreferencesManager - brukes av widgets via reflection-lignende oppslag
-keep class com.grevlingappen.data.PreferencesRepository { *; }

# EmailSender - sentral for app-funksjonalitet
-keep class com.grevlingappen.utils.EmailSender { *; }

# Logger - bruker BuildConfig reflection
-keep class com.grevlingappen.utils.Logger { *; }

# AppInfo - data class
-keep class com.grevlingappen.domain.models.AppInfo { *; }
-keep class com.grevlingappen.domain.models.ForwardingState { *; }

# ----------------------------------------------------------------------------
# DEBUG INFORMATION - For bedre crash reports
# ----------------------------------------------------------------------------
# Behold linje-nummer i stack traces (nyttig for debugging crashes)
-keepattributes SourceFile,LineNumberTable

# Skjul original filnavn (ekstra sikkerhet)
-renamesourcefileattribute SourceFile

# ----------------------------------------------------------------------------
# LOGGING - Fjern debug/verbose logging i release builds
# ----------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-assumenosideeffects class com.grevlingappen.utils.Logger {
    public static void d(...);
    public static void i(...);
}

# ----------------------------------------------------------------------------
# KOTLIN METADATA - Nødvendig for Kotlin reflection
# ----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# ----------------------------------------------------------------------------
# KOTLIN COROUTINES - For asynkron kode
# ----------------------------------------------------------------------------
# Behold volatile fields i coroutines (kritisk for threading)
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ----------------------------------------------------------------------------
# WARNINGS - Ignorer warnings for biblioteker vi ikke bruker
# ----------------------------------------------------------------------------
# Disse bibliotekene er valgfrie dependencies for JavaMail/Tink
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Google API Client (ikke brukt, men referert av JavaMail)
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport

# Annotations
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn javax.annotation.concurrent.ThreadSafe
-dontwarn org.joda.time.Instant

# ============================================================================
# NOTATER
# ============================================================================
#
# Hva er forskjellen på -keep, -keepclassmembers, og -keepnames?
# - -keep: Behold klassen OG alle members (metoder, fields)
# - -keepclassmembers: Behold kun spesifikke members, ikke nødvendigvis klassen
# - -keepnames: Behold navnet, men tillat at klassen fjernes hvis ubrukt
#
# Hva betyr { *; }?
# - { *; } = Behold ALT i klassen (alle metoder og fields)
# - { } = Behold bare klassen selv
#
# Hvorfor så mange -dontwarn?
# - JavaMail er designet for standard Java (desktop/server)
# - Den refererer til mange klasser som ikke finnes på Android
# - -dontwarn forteller ProGuard å ignorere disse manglende klassene
#
# Testing av ProGuard:
# 1. Bygg release APK: Build → Generate Signed Bundle / APK
# 2. Test alle funksjoner grundig (spesielt e-post sending!)
# 3. Sjekk Logcat for ClassNotFoundException eller NoSuchMethodException
#
# ============================================================================
