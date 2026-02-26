package com.grevlingappen.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.grevlingappen.data.PreferenceKeys

/**
 * EncryptedPrefsFactory - Factory som lager krypterte SharedPreferences.
 * 
 * Funksjonalitet:
 * - Lager krypterte SharedPreferences med AES-256 kryptering
 * - Håndterer automatisk gjenoppretting ved korrupsjon
 * - Bruker AndroidX Security-biblioteket
 * - Singleton-pattern med trådsikker initialisering
 * 
 * Viktig: Hvis kryptert lagring feiler, vises en dialog ved neste oppstart
 * som forklarer at data ble nullstilt.
 */
object EncryptedPrefsFactory {
    private const val TAG = "EncryptedPrefsFactory"
    
    // Singleton-instans med volatile for trådsikker tilgang
    @Volatile
    private var instance: SharedPreferences? = null

    /**
     * Henter singleton-instansen av krypterte innstillinger.
     * 
     * Bruker double-checked locking for optimal ytelse:
     * 1. Sjekk uten lock (hurtig path hvis allerede initialisert)
     * 2. Sjekk med lock (sikrer at kun én tråd lager instansen)
     * 
     * @param context App-kontekst
     * @return SharedPreferences med AES-256 kryptering
     */
    fun get(context: Context): SharedPreferences {
        val i = instance
        if (i != null) return i

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = createSafeInstance(context.applicationContext)
                instance = created
                created
            }
        }
    }

    /**
     * Oppretter krypterte SharedPreferences med feilhåndtering.
     * 
     * Hvis første forsøk feiler (f.eks. korrupt fil), prøves det på nytt
     * etter å ha slettet den korrupte fila.
     */
    private fun createSafeInstance(context: Context): SharedPreferences {
        return try {
            val masterKey = createMasterKey(context)
            createEncryptedPrefs(context, masterKey)
        } catch (e: Exception) {
            Logger.e(TAG, "Kryptert lagring feilet, prøver reset", e)
            
            // Sett flagg FØR vi sletter - UI viser forklaring ved neste oppstart
            context.getSharedPreferences("app_flags", Context.MODE_PRIVATE)
                .edit().putBoolean("prefs_were_reset", true).apply()
            
            // Slett korrupt fil via systemets API
            context.deleteSharedPreferences(PreferenceKeys.PREFS_NAME)
            
            try {
                // Prøv på nytt med fresh start
                val masterKey = createMasterKey(context)
                createEncryptedPrefs(context, masterKey)
            } catch (retryEx: Exception) {
                Logger.e(TAG, "Kritisk feil: Sikker lagring er utilgjengelig etter reset", retryEx)
                throw SecurityException("Kunne ikke opprette sikker lagring. Appen kan ikke fortsette.", retryEx)
            }
        }
    }

    /**
     * Oppretter MasterKey for kryptering.
     * Bruker AES-256-GCM som krypteringsalgoritme.
     */
    @Suppress("DEPRECATION")
    private fun createMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Oppretter krypterte SharedPreferences.
     * 
     * Bruker:
     * - AES-256-SIV for nøkler (ikontrollerer tilgang til verdier)
     * - AES-256-GCM for verdier (selve krypteringen)
     */
    @Suppress("DEPRECATION")
    private fun createEncryptedPrefs(context: Context, masterKey: MasterKey): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            PreferenceKeys.PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}