package com.dxlr8r.notifyme.ui.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.min
import kotlin.math.sqrt

data class HourglassColors(
    val frame: Color,
    val glass: Color,
    val sand: Color,
    val sandDark: Color,
)

/**
 * A desk hourglass. Sand drains from the top bulb to the bottom bulb in step with real time.
 *
 * @param endAt     epoch millis when the timer ends (> 0 while running)
 * @param pausedLeft millis left while paused (> 0 while paused)
 * @param totalMs   full length of the timer
 * @param finished  true once the timer has run out (all sand at the bottom)
 * @param flip      0..1 progress of the "turn the hourglass over" animation shown when a timer starts
 */
@Composable
fun Hourglass(
    endAt: Long,
    pausedLeft: Long,
    totalMs: Long,
    finished: Boolean,
    colors: HourglassColors,
    modifier: Modifier = Modifier,
    flip: Float = 0f,
) {
    val running = endAt > 0
    // Re-read the clock on every frame while running so the sand moves smoothly, not once a second.
    var frameNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running, endAt) {
        while (running) {
            withFrameMillis { frameNow = System.currentTimeMillis() }
        }
    }
    val remaining: Float = when {
        finished -> 0f
        running -> ((endAt - frameNow).toFloat() / totalMs.coerceAtLeast(1)).coerceIn(0f, 1f)
        pausedLeft > 0 -> (pausedLeft.toFloat() / totalMs.coerceAtLeast(1)).coerceIn(0f, 1f)
        else -> 1f
    }

    val grains = rememberInfiniteTransition(label = "grains")
    val phase by grains.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "phase",
    )

    Canvas(modifier) {
        rotate(degrees = flip * 180f) {
            drawHourglass(remaining, running && remaining > 0f, phase, colors)
        }
    }
}

private fun DrawScope.drawHourglass(remaining: Float, flowing: Boolean, phase: Float, c: HourglassColors) {
    // Fit a 0.62 : 1 hourglass in the available space.
    val hgH = min(size.height, size.width / 0.62f)
    val hgW = hgH * 0.62f
    val x0 = (size.width - hgW) / 2f
    val y0 = (size.height - hgH) / 2f
    val cx = size.width / 2f

    val capH = hgH * 0.055f
    val yT = y0 + capH
    val yB = y0 + hgH - capH
    val mid = (yT + yB) / 2f
    val gw = hgW * 0.74f
    val neck = gw * 0.075f
    val halfH = mid - yT

    val glass = Path().apply {
        moveTo(cx - gw / 2f, yT)
        lineTo(cx + gw / 2f, yT)
        cubicTo(cx + gw / 2f, yT + halfH * 0.58f, cx + neck / 2f, mid - halfH * 0.22f, cx + neck / 2f, mid)
        cubicTo(cx + neck / 2f, mid + halfH * 0.22f, cx + gw / 2f, yB - halfH * 0.58f, cx + gw / 2f, yB)
        lineTo(cx - gw / 2f, yB)
        cubicTo(cx - gw / 2f, yB - halfH * 0.58f, cx - neck / 2f, mid + halfH * 0.22f, cx - neck / 2f, mid)
        cubicTo(cx - neck / 2f, mid - halfH * 0.22f, cx - gw / 2f, yT + halfH * 0.58f, cx - gw / 2f, yT)
        close()
    }

    // Glass tint
    drawPath(glass, c.glass.copy(alpha = c.glass.alpha * 0.18f))

    // Sand. Bulbs narrow to the neck, so height follows sqrt(volume) to look physically right.
    val fill = 0.9f
    val topH = halfH * fill * sqrt(remaining)
    val botH = halfH * fill * (1f - sqrt(remaining))
    val sandBrush = Brush.verticalGradient(listOf(c.sand, c.sandDark))
    clipPath(glass) {
        if (topH > 0.5f) {
            val level = mid - topH
            val dip = if (flowing) min(topH * 0.18f, gw * 0.07f) else topH * 0.04f
            val top = Path().apply {
                moveTo(cx - gw, level)
                quadraticTo(cx, level + dip * 2f, cx + gw, level)
                lineTo(cx + gw, mid + 1f)
                lineTo(cx - gw, mid + 1f)
                close()
            }
            drawPath(top, sandBrush)
        }
        if (botH > 0.5f) {
            // A soft mound: edges lower than the centre, like real sand landing in the middle.
            val shoulder = yB - botH * 0.8f
            val apex = yB - botH * 1.2f
            val pile = Path().apply {
                moveTo(cx - gw, yB)
                lineTo(cx - gw, shoulder)
                quadraticTo(cx, 2f * apex - shoulder, cx + gw, shoulder)
                lineTo(cx + gw, yB)
                close()
            }
            drawPath(pile, sandBrush)
        }
        if (flowing) {
            val streamEnd = if (botH > 0.5f) yB - botH * 1.2f else yB
            val w = neck * 0.38f
            drawLine(c.sand, Offset(cx, mid - 2f), Offset(cx, streamEnd), strokeWidth = w)
            // Falling grains for a sense of motion
            val span = streamEnd - mid
            for (i in 0 until 6) {
                val t = ((i / 6f) + phase) % 1f
                drawCircle(c.sandDark, radius = w * 0.75f, center = Offset(cx + (if (i % 2 == 0) w * 0.4f else -w * 0.4f), mid + span * t))
            }
        }
    }

    // Glass outline + highlight
    val stroke = hgW * 0.014f
    drawPath(glass, c.glass, style = Stroke(width = stroke))
    val shine = Path().apply {
        moveTo(cx - gw * 0.36f, yT + halfH * 0.12f)
        cubicTo(cx - gw * 0.36f, yT + halfH * 0.45f, cx - gw * 0.2f, mid - halfH * 0.3f, cx - gw * 0.12f, mid - halfH * 0.18f)
    }
    drawPath(shine, Color.White.copy(alpha = 0.45f), style = Stroke(width = stroke * 1.4f, cap = StrokeCap.Round))

    // Wooden frame: caps and posts
    val r = CornerRadius(capH * 0.45f)
    drawRoundRect(c.frame, Offset(x0, y0), Size(hgW, capH), r)
    drawRoundRect(c.frame, Offset(x0, yB), Size(hgW, capH), r)
    val postW = hgW * 0.04f
    drawRoundRect(c.frame, Offset(x0 + hgW * 0.03f, yT), Size(postW, yB - yT), CornerRadius(postW / 2f))
    drawRoundRect(c.frame, Offset(x0 + hgW * 0.97f - postW, yT), Size(postW, yB - yT), CornerRadius(postW / 2f))
}
