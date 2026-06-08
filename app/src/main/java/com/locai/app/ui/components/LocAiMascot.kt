package com.locai.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * LocAi's friendly on-device companion: a small geometric robot drawn entirely with vector
 * primitives (no image assets to ship or keep in sync) — used to warm up onboarding and
 * "getting things ready" moments without relying on stock iconography.
 */
@Composable
fun LocAiMascot(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 140.dp) {
    val head = MaterialTheme.colorScheme.primary
    val face = MaterialTheme.colorScheme.onPrimary
    val spark = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Antenna: a small "always listening, locally" spark
        drawLine(
            color = head,
            start = Offset(w * 0.5f, h * 0.06f),
            end = Offset(w * 0.5f, h * 0.19f),
            strokeWidth = w * 0.028f,
            cap = StrokeCap.Round
        )
        drawCircle(color = spark, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.07f))

        // Head
        drawRoundRect(
            color = head,
            topLeft = Offset(w * 0.16f, h * 0.20f),
            size = Size(w * 0.68f, h * 0.64f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
        )

        // Face plate
        drawRoundRect(
            color = face.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.255f, h * 0.325f),
            size = Size(w * 0.49f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
        )

        // Eyes
        val eyeY = h * 0.475f
        drawCircle(color = face, radius = w * 0.052f, center = Offset(w * 0.385f, eyeY))
        drawCircle(color = face, radius = w * 0.052f, center = Offset(w * 0.615f, eyeY))

        // Smile
        drawArc(
            color = face,
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(w * 0.365f, h * 0.515f),
            size = Size(w * 0.27f, w * 0.17f),
            style = Stroke(width = w * 0.028f, cap = StrokeCap.Round)
        )
    }
}
