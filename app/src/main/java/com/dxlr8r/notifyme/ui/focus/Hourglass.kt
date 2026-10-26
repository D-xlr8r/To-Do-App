package com.dxlr8r.notifyme.ui.focus

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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class HourglassColors(
    val woodLight: Color,
    val woodDark: Color,
    val glass: Color,
    val sandLight: Color,
    val sand: Color,
    val sandDark: Color,
    val shadow: Color,
)

/**
 * A desk hourglass. Grains fall through the neck one by one and pile up in the lower bulb,
 * and the amount of sand in each bulb follows the real remaining time.
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
    var frameNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running, endAt) {
        while (running) withFrameMillis { frameNow = System.currentTimeMillis() }
    }
    val remaining: Float = when {
        finished -> 0f
        running -> ((endAt - frameNow).toFloat() / totalMs.coerceAtLeast(1)).coerceIn(0f, 1f)
        pausedLeft > 0 -> (pausedLeft.toFloat() / totalMs.coerceAtLeast(1)).coerceIn(0f, 1f)
        else -> 1f
    }
    val flowing = running && remaining > 0.002f

    Canvas(modifier) {
        rotate(degrees = flip * 180f) {
            drawHourglass(remaining, flowing, frameNow, colors)
        }
    }
}

// Fixed grain texture, in unit coordinates, so the sand looks granular but never flickers.
private class Speck(val x: Float, val y: Float, val r: Float, val tone: Int)
private val specks: List<Speck> = Random(42).let { rnd ->
    List(600) { Speck(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 0.9f, rnd.nextInt(3)) }
}

private fun smooth(x: Float) = x * x * (3f - 2f * x)

/**
 * A value in -1..1 that holds still, then shifts to a new value over about one second,
 * every [periodS] seconds — like sand slumping into a new shape.
 */
private fun slump(tS: Double, periodS: Double, salt: Int): Float {
    val step = floor(tS / periodS).toLong()
    val intoStep = ((tS - step * periodS)).toFloat()
    val a = noise(step, salt) * 2f - 1f
    val b = noise(step + 1, salt) * 2f - 1f
    return a + (b - a) * smooth(intoStep.coerceIn(0f, 1f))
}

/** Cheap deterministic noise in 0..1 for particle k. */
private fun noise(k: Long, salt: Int): Float {
    val v = sin(k * 12.9898 + salt * 78.233) * 43758.5453
    return (v - floor(v)).toFloat()
}

private fun DrawScope.drawHourglass(remaining: Float, flowing: Boolean, nowMs: Long, c: HourglassColors) {
    // Proportions of a classic desk timer
    val hgH = min(size.height, size.width / 0.6f)
    val hgW = hgH * 0.6f
    val x0 = (size.width - hgW) / 2f
    val y0 = (size.height - hgH) / 2f
    val cx = size.width / 2f

    val capH = hgH * 0.06f
    val yT = y0 + capH
    val yB = y0 + hgH - capH
    val mid = (yT + yB) / 2f
    val gw = hgW * 0.72f              // widest part of a bulb
    val neck = gw * 0.07f
    val halfH = mid - yT
    val unit = gw / 100f

    // ---- the glass outline: two rounded bulbs meeting at a narrow neck
    val glass = Path().apply {
        moveTo(cx - gw * 0.42f, yT)
        lineTo(cx + gw * 0.42f, yT)
        cubicTo(cx + gw * 0.56f, yT + halfH * 0.05f, cx + gw * 0.52f, yT + halfH * 0.55f, cx + neck / 2f, mid - halfH * 0.04f)
        lineTo(cx + neck / 2f, mid + halfH * 0.04f)
        cubicTo(cx + gw * 0.52f, yB - halfH * 0.55f, cx + gw * 0.56f, yB - halfH * 0.05f, cx + gw * 0.42f, yB)
        lineTo(cx - gw * 0.42f, yB)
        cubicTo(cx - gw * 0.56f, yB - halfH * 0.05f, cx - gw * 0.52f, yB - halfH * 0.55f, cx - neck / 2f, mid + halfH * 0.04f)
        lineTo(cx - neck / 2f, mid - halfH * 0.04f)
        cubicTo(cx - gw * 0.52f, yT + halfH * 0.55f, cx - gw * 0.56f, yT + halfH * 0.05f, cx - gw * 0.42f, yT)
        close()
    }

    // ---- soft shadow on the desk
    drawOval(
        Brush.radialGradient(listOf(c.shadow, Color.Transparent), center = Offset(cx, y0 + hgH), radius = hgW * 0.6f),
        topLeft = Offset(cx - hgW * 0.6f, y0 + hgH - capH * 0.4f),
        size = Size(hgW * 1.2f, capH * 1.3f),
    )

    // ---- glass body (faint tint so the empty bulb still reads as glass)
    drawPath(glass, Brush.horizontalGradient(
        listOf(c.glass.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f), c.glass.copy(alpha = 0.16f)),
        startX = cx - gw / 2f, endX = cx + gw / 2f,
    ))

    // ---- sand volumes: bulbs taper to the neck, so height ~ sqrt(volume)
    val topH = halfH * 0.86f * sqrt(remaining)
    val botH = halfH * 0.86f * (1f - sqrt(remaining))
    val sandBrush = Brush.verticalGradient(listOf(c.sandLight, c.sand, c.sandDark), startY = yT, endY = yB)

    val tS = nowMs / 1000.0

    // Top: a level surface with a funnel over the neck. While sand flows the surface slowly
    // re-shapes itself every few seconds, the way real sand slumps and slides.
    val topSand = Path()
    val level = mid - topH
    val craterDepth = if (flowing) min(topH * 0.92f, halfH * (0.20f + 0.04f * slump(tS, 3.2, 13))) else 0f
    val s1 = if (flowing) slump(tS, 4.0, 11) else 0f
    val s2 = if (flowing) slump(tS, 5.5, 12) else 0f
    val craterW = gw * (0.30f + 0.05f * s1)
    val off = gw * 0.025f * s2
    val tilt = topH * 0.04f * s1
    val wallLift = if (flowing) min(topH * 0.10f, gw * 0.03f) else 0f
    if (topH > 0.5f) {
        topSand.apply {
            moveTo(cx - gw, level - wallLift - tilt)
            quadraticTo(cx - gw * 0.45f, level - tilt * 0.5f, cx + off - craterW, level - tilt * 0.3f)
            cubicTo(cx + off - craterW * 0.45f, level, cx - neck, level + craterDepth, cx, level + craterDepth)
            cubicTo(cx + neck, level + craterDepth, cx + off + craterW * 0.45f, level, cx + off + craterW, level + tilt * 0.3f)
            quadraticTo(cx + gw * 0.45f, level + tilt * 0.5f, cx + gw, level - wallLift + tilt)
            lineTo(cx + gw, mid + 2f)
            lineTo(cx - gw, mid + 2f)
            close()
        }
    }

    // Bottom: a low mound that spreads out to the glass walls as grains land
    val bottomSand = Path()
    var peakY = yB
    var wallY = yB
    if (botH > 0.5f) {
        wallY = yB - botH * 0.88f
        peakY = yB - botH * 1.12f
        val tip = gw * 0.06f
        bottomSand.apply {
            moveTo(cx - gw, yB + 2f)
            lineTo(cx - gw, wallY)
            quadraticTo(cx - gw * 0.25f, wallY - (wallY - peakY) * 0.35f, cx - tip, peakY + tip * 0.3f)
            quadraticTo(cx, peakY - tip * 0.1f, cx + tip, peakY + tip * 0.3f)
            quadraticTo(cx + gw * 0.25f, wallY - (wallY - peakY) * 0.35f, cx + gw, wallY)
            lineTo(cx + gw, yB + 2f)
            close()
        }
    }
    fun surfaceAt(dx: Float): Float =
        if (botH > 0.5f) peakY + (wallY - peakY) * min(abs(dx) / (gw * 0.45f), 1f) else yB

    clipPath(glass) {
        for ((path, has) in listOf(topSand to (topH > 0.5f), bottomSand to (botH > 0.5f))) {
            if (!has) continue
            drawPath(path, sandBrush)
            clipPath(path) {
                for (s in specks) {
                    val col = when (s.tone) { 0 -> c.sandDark.copy(alpha = 0.45f); 1 -> c.sandLight.copy(alpha = 0.55f); else -> c.sand }
                    drawCircle(col, radius = s.r * unit * 0.55f, center = Offset(cx - gw / 2f + s.x * gw, yT + s.y * (yB - yT)))
                }
            }
        }

        // ---- grains sliding down the funnel walls in the top bulb
        if (flowing && topH > halfH * 0.05f) {
            for (j in 0 until 10) {
                val period = 1.4
                val u = (((tS + noise(j.toLong(), 20) * period) / period) % 1.0).toFloat()
                val side = if (j % 2 == 0) -1f else 1f
                val startX = cx + off + side * craterW * (0.75f + 0.2f * noise(j.toLong(), 21))
                val x = startX + (cx - startX) * u * sqrt(u)
                val y = level + craterDepth * (0.15f + 0.8f * u) - unit * 0.4f
                drawCircle(c.sandDark.copy(alpha = 0.8f * (1f - u * 0.5f)), radius = unit * 0.7f, center = Offset(x, y))
            }
        }

        // ---- falling grains: individual particles that fall, hit the bottom, bounce up a little
        // and then scatter towards the left and right corners of the glass
        if (flowing) {
            val landY = surfaceAt(0f)
            val dist = (landY - mid).coerceAtLeast(1f)
            val g = 2f * halfH / (0.62f * 0.62f)          // px/s², a full drop takes ~0.62 s
            val fallS = sqrt(2f * dist / g)
            val life = 0.9f                                // time a landed grain stays visible
            val hopT = 0.14f
            val spawnMs = 9.0
            val lastK = floor(nowMs / spawnMs).toLong()
            val firstK = lastK - ((fallS + life) * 1000f / spawnMs).toLong() - 5
            for (k in firstK..lastK) {
                val age = ((nowMs - k * spawnMs) / 1000.0).toFloat()
                if (age < 0f) continue
                val fall = 0.5f * g * age * age
                val r = unit * (0.55f + noise(k, 3) * 0.6f)
                val col = when ((noise(k, 4) * 3).toInt()) { 0 -> c.sandDark; 1 -> c.sandLight; else -> c.sand }
                if (fall < dist) {
                    val spread = neck * 0.22f + neck * 0.35f * (fall / dist)
                    val x = cx + (noise(k, 1) - 0.5f) * spread
                    drawCircle(col, radius = r, center = Offset(x, mid + fall))
                } else {
                    val t = age - fallS
                    if (t > life) continue
                    val dir = if (noise(k, 5) < 0.5f) -1f else 1f
                    val n6 = noise(k, 6)
                    val reach = gw * 0.45f * n6 * sqrt(n6) + gw * 0.04f   // most stay near, some run to the corners
                    val dx: Float
                    val y: Float
                    if (t < hopT) {
                        val u = t / hopT
                        dx = dir * reach * 0.18f * u
                        y = surfaceAt(dx) - r - unit * (2.5f + 4f * noise(k, 7)) * sin(PI.toFloat() * u)
                    } else {
                        val u = (t - hopT) / (life - hopT)
                        val ease = 1f - (1f - u) * (1f - u)
                        dx = dir * reach * (0.18f + 0.82f * ease)
                        y = surfaceAt(dx) - r
                    }
                    val alpha = if (t > life * 0.7f) 1f - (t - life * 0.7f) / (life * 0.3f) else 1f
                    drawCircle(col.copy(alpha = alpha.coerceIn(0f, 1f)), radius = r, center = Offset(cx + dx, y))
                }
            }
        }
    }

    // ---- glass: rim, outline and reflections
    val stroke = hgW * 0.011f
    drawPath(glass, c.glass.copy(alpha = 0.75f), style = Stroke(width = stroke))
    val shineL = Path().apply {
        moveTo(cx - gw * 0.38f, yT + halfH * 0.10f)
        cubicTo(cx - gw * 0.46f, yT + halfH * 0.35f, cx - gw * 0.30f, mid - halfH * 0.30f, cx - gw * 0.10f, mid - halfH * 0.12f)
        moveTo(cx - gw * 0.10f, mid + halfH * 0.12f)
        cubicTo(cx - gw * 0.30f, mid + halfH * 0.30f, cx - gw * 0.46f, yB - halfH * 0.35f, cx - gw * 0.38f, yB - halfH * 0.10f)
    }
    drawPath(shineL, Color.White.copy(alpha = 0.55f), style = Stroke(width = stroke * 2.2f, cap = StrokeCap.Round))
    val shineR = Path().apply {
        moveTo(cx + gw * 0.40f, yT + halfH * 0.18f)
        cubicTo(cx + gw * 0.44f, yT + halfH * 0.32f, cx + gw * 0.38f, yT + halfH * 0.45f, cx + gw * 0.30f, yT + halfH * 0.55f)
    }
    drawPath(shineR, Color.White.copy(alpha = 0.35f), style = Stroke(width = stroke * 1.2f, cap = StrokeCap.Round))

    // ---- wooden frame: turned posts and thick caps with a bevel
    val wood = Brush.horizontalGradient(listOf(c.woodDark, c.woodLight, c.woodDark), startX = x0, endX = x0 + hgW)
    val postW = hgW * 0.045f
    for (px in listOf(x0 + hgW * 0.06f, x0 + hgW * 0.94f - postW)) {
        val post = Brush.horizontalGradient(listOf(c.woodDark, c.woodLight, c.woodDark), startX = px, endX = px + postW)
        drawRoundRect(post, Offset(px, yT), Size(postW, yB - yT), CornerRadius(postW / 2f))
        // little turned rings near the ends
        for (ry in listOf(yT + capH * 0.6f, yB - capH * 0.6f, mid)) {
            drawRoundRect(post, Offset(px - postW * 0.25f, ry - postW * 0.35f), Size(postW * 1.5f, postW * 0.7f), CornerRadius(postW * 0.35f))
        }
    }
    val r = CornerRadius(capH * 0.35f)
    drawRoundRect(wood, Offset(x0, y0), Size(hgW, capH), r)
    drawRoundRect(wood, Offset(x0, yB), Size(hgW, capH), r)
    // bevel highlights
    drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(x0 + capH * 0.3f, y0 + capH * 0.12f), Size(hgW - capH * 0.6f, capH * 0.18f), CornerRadius(capH * 0.1f))
    drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(x0 + capH * 0.3f, yB + capH * 0.12f), Size(hgW - capH * 0.6f, capH * 0.18f), CornerRadius(capH * 0.1f))
}
