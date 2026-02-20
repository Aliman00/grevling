package com.grevlingappen.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grevlingappen.R
import com.grevlingappen.domain.models.StatusColor
import com.grevlingappen.ui.theme.StatusActive
import com.grevlingappen.ui.theme.StatusPaused
import com.grevlingappen.ui.theme.StatusWarning

// ============================================================================
// STATUS CIRCLE - Avansert animert status-indikator
// ============================================================================

@Composable
fun StatusCircle(
    status: StatusColor,
    modifier: Modifier = Modifier
) {
    val isActive = status == StatusColor.ACTIVE
    
    // ------------------------------------------------------------------------
    // ANIMASJONS-KONFIGURASJON
    // ------------------------------------------------------------------------
    val infiniteTransition = rememberInfiniteTransition(label = "super_pulse")
    
    // Animert fargeskifte (mellom Aktiv/Pauset/Varsel)
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            StatusColor.ACTIVE -> StatusActive
            StatusColor.WARNING -> StatusWarning
            StatusColor.PAUSED -> StatusPaused
        },
        animationSpec = tween(durationMillis = 800),
        label = "statusColor"
    )
    
    // 1. Pulserende skala-effekt (KUN når aktiv)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = if (isActive) RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = "scale"
    )

    // 2. Pulserende glød-alpha (KUN når aktiv)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isActive) 0.6f else 0.3f, // Fast verdi (0.3) via alpha-mating når ikke aktiv
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    // 3. Kontinuerlig rotasjon for kant-detalj (KUN når aktiv)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // ------------------------------------------------------------------------
    // LAYOUT
    // ------------------------------------------------------------------------
    Box(
        modifier = modifier
            .size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Roterende kant-ring (Vises kun når aktiv)
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .rotate(rotation)
                    .drawBehind {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, statusColor, Color.Transparent)
                            ),
                            startAngle = 0f,
                            sweepAngle = 120f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            )
        }

        // Selve sirkelen med INTERN glød/dybde
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isActive) {
                            listOf(
                                Color(0xFFE0FFF0).copy(alpha = glowAlpha), // Lys kjerne
                                statusColor.copy(alpha = 0.1f)             // Dempet kant
                            )
                        } else {
                            listOf(
                                statusColor.copy(alpha = 0.15f), // Statisk dyp rød kjerne
                                statusColor.copy(alpha = 0.05f)  // Nesten gjennomsiktig kant
                            )
                        }
                    )
                )
                .border(
                    width = 3.dp,
                    color = statusColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Animert tekst-overgang
            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith 
                    fadeOut(animationSpec = tween(500))
                },
                label = "textTransition"
            ) { targetStatus ->
                Text(
                    text = when (targetStatus) {
                        StatusColor.ACTIVE -> stringResource(R.string.status_circle_active_label)
                        StatusColor.WARNING -> stringResource(R.string.status_circle_warning_label)
                        StatusColor.PAUSED -> stringResource(R.string.status_circle_paused_label)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
