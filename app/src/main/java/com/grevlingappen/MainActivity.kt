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
 * Setter opp Compose-runtime og viser hovedskjermen med appens tema.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_FLAGS = "app_flags"
        private const val KEY_PREFS_RESET = "prefs_were_reset"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sjekk om prefs ble reset tidligere
        val prefsWereReset = getSharedPreferences(PREFS_FLAGS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREFS_RESET, false)

        if (prefsWereReset) {
            // Clear the flag
            getSharedPreferences(PREFS_FLAGS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_PREFS_RESET, false).apply()

            // Show dialog - do this BEFORE setContent
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_prefs_reset_title))
                .setMessage(getString(R.string.dialog_prefs_reset_message))
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

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