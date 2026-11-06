/*
 * SPDX-FileCopyrightText: The petalOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/** What the update is doing. The bloom is the app's status display, not a backdrop. */
sealed interface BloomState {
    data object Resting : BloomState
    data object Searching : BloomState

    /** An update is waiting: the three centre petals light up. */
    data object Ready : BloomState
    data class Progress(val fraction: Float, val paused: Boolean = false) : BloomState
    data object Finished : BloomState
    data object Failed : BloomState
}

// Same petal as the bloom on petalos.xyz, and the same three rings
private val PETAL = Path().apply {
    moveTo(0f, 0f)
    cubicTo(-46f, -120f, -128f, -250f, -96f, -392f)
    cubicTo(-76f, -480f, -28f, -536f, 0f, -566f)
    cubicTo(28f, -536f, 76f, -480f, 96f, -392f)
    cubicTo(128f, -250f, 46f, -120f, 0f, 0f)
    close()
}
private const val PETAL_LENGTH = 566f

private class Ring(val count: Int, val step: Float, val size: Float, val middle: Float)

private val RINGS = listOf(
    Ring(count = 13, step = 14f, size = 1.72f, middle = 0.62f),
    Ring(count = 11, step = 16f, size = 1.28f, middle = 0.60f),
    Ring(count = 9, step = 19f, size = 0.88f, middle = 0.55f),
)

private class Petal(val ring: Int, val angle: Float, var rank: Int = 0)

// Ranked by angle across all three rings, so progress sweeps left to right over the whole bloom
private val PETALS = RINGS.flatMapIndexed { index, ring ->
    List(ring.count) { i -> Petal(index, (i - (ring.count - 1) / 2f) * ring.step) }
}.also { petals ->
    petals.sortedBy { it.angle }.forEachIndexed { rank, petal -> petal.rank = rank }
}

private class BloomColors(
    val rings: List<List<Color>>,
    val line: Color,
    val glow: List<Color>,
    val glowLine: Color,
)

private val DARK = BloomColors(
    rings = listOf(
        listOf(Color(0xFF6B4BE0), Color(0xB83A2AA0), Color(0x002A1F7A)),
        listOf(Color(0xFFA274F2), Color(0x9E5E43C8), Color(0x004A36AE)),
        listOf(Color(0xFFFFD9EC), Color(0x9EC39DF0), Color(0x008A6FD0)),
    ),
    line = Color(0x6BCDBEFF),
    glow = listOf(Color(0xD1FFF4FA), Color(0x9EE9D6FF), Color(0x6BC7B2FF)),
    glowLine = Color(0xFFF6EEFF),
)

private val LIGHT = BloomColors(
    rings = listOf(
        listOf(Color(0xFFBCA6FF), Color(0xB8DCD0FF), Color(0x00EFE8FF)),
        listOf(Color(0xFFFFB3D9), Color(0x9ECDB8FF), Color(0x00E8DEFF)),
        listOf(Color(0xFF7C4DFF), Color(0x9EB79BFF), Color(0x00E9C6E6)),
    ),
    line = Color(0x706938EF),
    glow = listOf(Color(0xD17B3FF2), Color(0x9E9A6BFF), Color(0x6BB99BFF)),
    glowLine = Color(0xFF6D2FEF),
)

private const val REST_ALPHA = 0.7f
private const val LIT_FILL = 0.72f

@Composable
fun PetalBloom(state: BloomState, modifier: Modifier = Modifier) {
    val colors = if (isSystemInDarkTheme()) DARK else LIGHT
    val animates = remember { ValueAnimator.areAnimatorsEnabled() }

    val target = when (state) {
        is BloomState.Progress -> state.fraction.coerceIn(0f, 1f)
        BloomState.Finished -> 1f
        else -> 0f
    }
    val filled by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(if (animates) 300 else 0),
        label = "bloom fill",
    )

    // One traveller, so "searching" can't be mistaken for progress. A still bloom without motion
    val wave = if (state == BloomState.Searching && animates) {
        val transition = rememberInfiniteTransition(label = "bloom search")
        val sweep by transition.animateFloat(
            initialValue = -0.2f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                tween(1800, easing = LinearEasing),
                RepeatMode.Reverse,
            ),
            label = "bloom sweep",
        )
        sweep
    } else {
        null
    }

    // The words carry the state for a screen reader, this only repeats them
    Canvas(modifier.clearAndSetSemantics {}) {
        val paused = state is BloomState.Progress && state.paused
        val failed = state == BloomState.Failed
        // the longest petal just reaches the top edge, tips stay inside the panel
        val unit = size.height / (PETAL_LENGTH * RINGS[0].size)
        translate(size.width / 2f, size.height) {
            PETALS.forEach { petal ->
                val lit = when {
                    state == BloomState.Ready -> if (petal.angle == 0f) 1f else 0f
                    wave != null -> max(0f, 1f - abs(petal.rank / 32f - wave) * 5f)
                    else -> (filled * PETALS.size - petal.rank).coerceIn(0f, 1f)
                }
                drawPetal(
                    petal = petal,
                    unit = unit,
                    lit = if (paused) lit * 0.55f else lit,
                    rest = if (failed) REST_ALPHA * 0.45f else REST_ALPHA,
                    colors = colors,
                )
            }
        }
    }
}

private fun DrawScope.drawPetal(petal: Petal, unit: Float, lit: Float, rest: Float, colors: BloomColors) {
    val ring = RINGS[petal.ring]
    val pixels = unit * ring.size
    val end = Offset(0f, -PETAL_LENGTH)
    val line = 1.dp.toPx() / pixels

    rotate(petal.angle, pivot = Offset.Zero) {
        scale(pixels, pixels, pivot = Offset.Zero) {
            val stops = colors.rings[petal.ring]
            drawPath(
                PETAL,
                Brush.linearGradient(
                    0f to stops[0],
                    ring.middle to stops[1],
                    1f to stops[2],
                    start = Offset.Zero,
                    end = end,
                ),
                alpha = rest,
            )
            drawPath(PETAL, colors.line, alpha = rest, style = Stroke(line))
            if (lit <= 0f) return@scale

            drawPath(
                PETAL,
                Brush.linearGradient(
                    0f to colors.glow[0],
                    0.55f to colors.glow[1],
                    1f to colors.glow[2],
                    start = Offset.Zero,
                    end = end,
                ),
                alpha = lit * LIT_FILL,
            )
            // soft halo stepping in to a hot edge
            listOf(12f to 0.05f, 7f to 0.09f, 3.5f to 0.16f, 1.25f to 0.9f).forEach { (width, strength) ->
                drawPath(
                    PETAL,
                    colors.glowLine,
                    alpha = lit * strength,
                    style = Stroke(line * width, join = StrokeJoin.Round),
                )
            }
        }
    }
}
