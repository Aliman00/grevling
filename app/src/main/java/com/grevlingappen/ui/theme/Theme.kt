package com.grevlingappen.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.grevlingappen.R

/**
 * GrevlingTheme - Hovedtema for appen.
 * 
 * Setter opp Material Design 3 farger og typografi for hele appen.
 * Wrapper hele appen i GrevlingTheme { } for å gi alle komponenter
 * tilgang til MaterialTheme.colorScheme.primary, .typography.bodyLarge, etc.
 * 
 * Brukes i MainActivity slik:
 * GrevlingTheme { YourScreen() }
 * 
 * @param darkTheme Om mørkt tema skal brukes (for fremtidig støtte)
 * @param content Alt UI som wrappes i temaet
 */
@Composable
fun GrevlingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // lightColorScheme() henter farger fra colors.xml for konsistens
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

    // Sett status bar (topp-menylinje med klokke/batteri)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Hvit bakgrunn for bedre kontrast mot ikoner
            window.statusBarColor = android.graphics.Color.WHITE
            // Mørke ikoner på lys bakgrunn
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    // MaterialTheme gir alle child-komponenter tilgang til farger og typografi
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}