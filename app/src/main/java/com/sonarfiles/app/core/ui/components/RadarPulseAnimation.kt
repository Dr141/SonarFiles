package com.sonarfiles.app.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.SonarCyanSecondary

@Composable
fun RadarPulseAnimation(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true,
    radarSize: Dp = 260.dp,
    discoveredHostCount: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransitions")

    // Rotação do feixe do radar (0 a 360 graus)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    // Pulso de onda expansiva concêntrica
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseFraction"
    )

    Box(
        modifier = modifier.size(radarSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // 1. Círculos concêntricos de grade do radar
            val ringCount = 4
            for (i in 1..ringCount) {
                val radius = maxRadius * (i.toFloat() / ringCount)
                drawCircle(
                    color = SonarCyanPrimary.copy(alpha = 0.18f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 2. Linhas de mira em cruz
            drawLine(
                color = SonarCyanPrimary.copy(alpha = 0.15f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = SonarCyanPrimary.copy(alpha = 0.15f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // 3. Onda expansiva pulsante
            if (isScanning) {
                val waveRadius = maxRadius * pulseFraction
                val waveAlpha = (1f - pulseFraction).coerceIn(0f, 1f) * 0.45f
                drawCircle(
                    color = SonarCyanPrimary.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // 4. Feixe rotativo cônico (Radar Sweep Cone)
                rotate(degrees = sweepAngle, pivot = center) {
                    val sweepBrush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.75f to Color.Transparent,
                        0.95f to SonarCyanSecondary.copy(alpha = 0.25f),
                        1.0f to SonarCyanPrimary.copy(alpha = 0.65f),
                        center = center
                    )
                    drawCircle(
                        brush = sweepBrush,
                        radius = maxRadius,
                        center = center
                    )
                }
            }

            // 5. Ponto central (Emissor do Sonar)
            drawCircle(
                color = SonarCyanPrimary,
                radius = 5.dp.toPx(),
                center = center
            )
        }
    }
}
