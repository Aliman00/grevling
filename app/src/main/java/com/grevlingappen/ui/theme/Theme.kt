package com.grevlingappen.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.grevlingappen.R

// ============================================================================
// THEME.KT - App-tema som kombinerer farger og typography
// ============================================================================
// Wrapper hele appen i GrevlingTheme { } for å gi alle komponenter
// tilgang til MaterialTheme.colorScheme.primary, .typography.bodyLarge, etc.

// ----------------------------------------------------------------------------
// GREVLING THEME - Hovedfunksjon som aktiverer temaet
// ----------------------------------------------------------------------------
// Brukes i MainActivity: GrevlingTheme { YourScreen() }
@Composable
fun GrevlingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // For fremtidig dark mode
    content: @Composable () -> Unit              // Alt UI wrappes i denne
) {
    // lightColorScheme() organiserer fargene i Material Design 3 standard
    // Henter farger direkte fra colors.xml for å ha "Single Source of Truth"
    val colorScheme = lightColorScheme(
        primary = colorResource(R.color.md_primary),
        onPrimary = colorResource(R.color.md_on_primary),
        primaryContainer = colorResource(R.color.md_primary_container),
        onPrimaryContainer = colorResource(R.color.md_on_primary_container),
        secondary = colorResource(R.color.md_secondary),
        onSecondary = colorResource(R.color.md_on_secondary),
        secondaryContainer = colorResource(R.color.md_secondary_container),
        onSecondaryContainer = colorResource(R.color.md_on_secondary_container),
        background = colorResource(R.color.md_background),
        surface = colorResource(R.color.md_surface),
        onSurface = colorResource(R.color.md_on_surface),
        surfaceVariant = colorResource(R.color.md_surface_variant),
        onSurfaceVariant = colorResource(R.color.md_on_surface_variant),
        outline = colorResource(R.color.md_outline),
        outlineVariant = colorResource(R.color.md_outline_variant)
    )

    // Sett status bar styling (top bar med klokke/batteri)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar får samme farge som bakgrunn (seamless)
            window.statusBarColor = colorScheme.background.toArgb()
            // Bruk mørke ikoner (klokke/batteri) på lys bakgrunn, og omvendt
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // MaterialTheme gir alle child-komponenter tilgang til colorScheme og typography
    MaterialTheme(
        colorScheme = colorScheme,  // Fargene fra LightColorScheme
        typography = Typography,    // Tekststiler fra Type.kt
        content = content           // Alt UI (screens, komponenter)
    )
}
