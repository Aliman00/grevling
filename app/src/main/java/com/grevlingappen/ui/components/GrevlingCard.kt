package com.grevlingappen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// GREVLING CARD - Standard kort-komponent brukt i hele appen
// ============================================================================
// Erstatter <MaterialCardView> fra XML med konsistent styling:
// - 16dp rounded corners
// - Ingen elevation (flat Material 3 design)
// - 1dp outline for synlig kant
// - 16dp standard padding (kan overstyres)

@Composable
fun GrevlingCard(
    modifier: Modifier = Modifier.fillMaxWidth(),
    internalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large, // 16dp rounded corners
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder() // 1dp outline
    ) {
        Column(
            modifier = Modifier.padding(internalPadding),
            content = content
        )
    }
}
