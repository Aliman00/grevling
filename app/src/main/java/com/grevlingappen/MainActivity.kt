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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sjekk om prefs ble reset tidligere
        val prefsWereReset = getSharedPreferences("app_flags", Context.MODE_PRIVATE)
            .getBoolean("prefs_were_reset", false)

        if (prefsWereReset) {
            // Clear the flag
            getSharedPreferences("app_flags", Context.MODE_PRIVATE)
                .edit().putBoolean("prefs_were_reset", false).apply()

            // Show dialog - do this BEFORE setContent
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Innstillinger tilbakestilt")
                    .setMessage("Sikker lagring måtte tilbakestilles på grunn av en feil. Vennligst konfigurer appen på nytt.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
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