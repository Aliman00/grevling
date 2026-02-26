package com.grevlingappen.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * GrevlingButton - Standard knapp brukt i hele appen.
 * 
 * Material 3 Button med konsistent styling:
 * - Ekstra store hjørner (pill-shape)
 * - 56dp høyde for god touch-target
 * - Bold/STOR tekst
 * 
 * @param text Tekst som vises på knappen
 * @param onClick Lambda som kalles ved klikk
 * @param modifier Modifier for tilpasning
 * @param enabled Om knappen er aktiv (true) eller disabled (false)
 * @param backgroundColor Bakgrunnsfarge (bruker tema primary som standard)
 * @param contentPadding Indre padding
 */
@Composable
fun GrevlingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding
) {
    Button(
        onClick = onClick,
        // Min-height på 56dp for god touch-target
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        enabled = enabled,
        // Bruk temaets ekstra store hjørne-radius
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        ),
        contentPadding = contentPadding
    ) {
        Text(
            text = text.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelLarge
        )
    }
}