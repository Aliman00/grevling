package com.grevlingappen

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.grevlingappen.ui.main.MainScreen
import com.grevlingappen.ui.theme.GrevlingTheme

/**
 * MainActivity - Inngangspunktet for GrevlingAppen.
 * 
 * Setter opp:
 * - Compose-runtime for Jetpack Compose UI
 * - App-tema (GrevlingTheme)
 * - Hovedskjerm (MainScreen)
 * 
 * Hvis kryptert lagring feilet ved forrige oppstart (og ble nullstilt),
 * vises en dialog som forklarer dette til brukeren.
 */
class MainActivity : ComponentActivity() {

    companion object {
        // Filnavn for app-flagg (atskilt fra hovedinnstillinger)
        private const val PREFS_FLAGS = "app_flags"
        // Nøkkel for "prefs ble nullstilt"
        private const val KEY_PREFS_RESET = "prefs_were_reset"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sjekk om preferanser ble nullstilt ved forrige oppstart
        val prefsWereReset = getSharedPreferences(PREFS_FLAGS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREFS_RESET, false)

        if (prefsWereReset) {
            // Fjern flagget så det ikke vises igjen
            getSharedPreferences(PREFS_FLAGS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_PREFS_RESET, false).apply()

            // Vis dialog FØR setContent (viktig for at dialog skal vises)
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_prefs_reset_title))
                .setMessage(getString(R.string.dialog_prefs_reset_message))
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Sett opp Compose med app-tema
        setContent {
            GrevlingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}