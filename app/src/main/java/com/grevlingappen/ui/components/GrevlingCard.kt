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

/**
 * GrevlingCard - Standard kort-komponent brukt i hele appen.
 * 
 * Material 3 Card med konsistent styling:
 * - 16dp rounded corners (store hjørner)
 * - Flat Material 3 design (ingen skygge)
 * - 1dp outline for synlig kant
 * - 16dp standard padding
 * 
 * @param modifier Modifier for ytre tilpasning
 * @param internalPadding Indre padding (standard 16dp)
 * @param content Innholdet i kortet
 */
@Composable
fun GrevlingCard(
    modifier: Modifier = Modifier.fillMaxWidth(),
    internalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        // 16dp rounded corners
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        // 1dp outline for synlig kant
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(internalPadding),
            content = content
        )
    }
}