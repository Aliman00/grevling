package com.grevlingappen.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.grevlingappen.data.PreferenceKeys

/**
 * Factory for EncryptedSharedPreferences.
 * Håndterer sikker lagring og automatisk gjenoppretting ved fil-korrupsjon.
 */
object EncryptedPrefsFactory {
    private const val TAG = "EncryptedPrefsFactory"
    
    @Volatile
    private var instance: SharedPreferences? = null

    /**
     * Hent trådsikker instans av krypterte innstillinger.
     * Bruker double-checked locking for optimal ytelse.
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

    private fun createSafeInstance(context: Context): SharedPreferences {
        return try {
            val masterKey = createMasterKey(context)
            createEncryptedPrefs(context, masterKey)
        } catch (e: Exception) {
            Logger.e(TAG, "Kryptert lagring feilet, prøver reset", e)
            
            // Slett korrupt fil via systemets API
            context.deleteSharedPreferences(PreferenceKeys.PREFS_NAME)
            
            try {
                val masterKey = createMasterKey(context)
                createEncryptedPrefs(context, masterKey)
            } catch (retryEx: Exception) {
                Logger.e(TAG, "Kritisk feil: Sikker lagring er utilgjengelig etter reset", retryEx)
                throw SecurityException("Kunne ikke opprette sikker lagring. Appen kan ikke fortsette av sikkerhetshensyn.", retryEx)
            }
        }
    }

    private fun createMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

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
