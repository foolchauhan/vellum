package com.example.vellum.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vellum.theme.ChalkboardSlate
import com.example.vellum.theme.ParchmentLine
import kotlin.random.Random

@Composable
fun ChalkboardBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val bgCol = colorScheme.background
    val isDark = bgCol != Color(0xFFFAF3E0) && bgCol != Color(0xFFEFEFEF) && bgCol != Color(0xFFD0E6F8)

    Box(
        modifier = Modifier
            .background(colorScheme.background)
            .then(modifier)
    ) {
        if (isDark) {
            Image(
                painter = painterResource(id = com.example.vellum.R.drawable.blackboard_background_01),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (bgCol == ChalkboardSlate) 1.0f else 0.18f,
                modifier = Modifier.matchParentSize()
            )
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            val random = Random(42) // Seeded for consistency
            
            when (bgCol) {
                Color(0xFFEFEFEF) -> {
                    // --- CEMENT (LIGHT CONCRETE) TEXTURE ---
                    // 1. Air pores
                    for (i in 0..70) {
                        val cx = random.nextFloat() * size.width
                        val cy = random.nextFloat() * size.height
                        val r = random.nextFloat() * 4.5f + 1f
                        drawCircle(
                            color = Color(0xFF555555),
                            radius = r,
                            center = Offset(cx, cy),
                            alpha = random.nextFloat() * 0.08f + 0.02f
                        )
                    }

                    // 2. Trowel sweeps
                    for (i in 0..12) {
                        val cx = random.nextFloat() * size.width
                        val cy = random.nextFloat() * size.height
                        val rx = random.nextFloat() * 300f + 100f
                        val ry = rx * 0.35f
                        drawOval(
                            color = if (random.nextBoolean()) Color.White else Color(0xFFB0B0B0),
                            topLeft = Offset(cx - rx, cy - ry),
                            size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
                            alpha = 0.06f
                        )
                    }

                    // 3. Concrete cracks
                    for (c in 1..3) {
                        var x0 = random.nextFloat() * size.width
                        var y0 = random.nextFloat() * (size.height * 0.6f)
                        val steps = random.nextInt(10) + 12
                        val dx = random.nextFloat() * 24f - 12f
                        val dy = random.nextFloat() * 35f + 15f

                        val crackPath = androidx.compose.ui.graphics.Path()
                        crackPath.moveTo(x0, y0)
                        for (j in 1..steps) {
                            x0 += dx + (random.nextFloat() * 16f - 8f)
                            y0 += dy + (random.nextFloat() * 10f - 5f)
                            crackPath.lineTo(x0, y0)
                        }
                        drawPath(
                            path = crackPath,
                            color = Color(0xFF888888),
                            alpha = 0.22f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.9f)
                        )
                    }
                }
                Color(0xFFD0E6F8) -> {
                    // --- FROSTED GLASS & WATER DROPLETS ---
                    // 1. Soft white frost spots
                    for (i in 0..25) {
                        val cx = random.nextFloat() * size.width
                        val cy = random.nextFloat() * size.height
                        val r = random.nextFloat() * 160f + 60f
                        drawCircle(
                            color = Color.White,
                            radius = r,
                            center = Offset(cx, cy),
                            alpha = 0.16f
                        )
                    }

                    // 2. Water droplets (3D)
                    for (d in 0..22) {
                        val cx = random.nextFloat() * size.width
                        val cy = random.nextFloat() * size.height
                        val r = random.nextFloat() * 16f + 7f

                        // Shadow on bottom-right
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = r,
                            center = Offset(cx + r * 0.12f, cy + r * 0.12f),
                            alpha = 0.24f
                        )

                        // Specular body
                        drawCircle(
                            color = Color(0xFFF1F5F9),
                            radius = r,
                            center = Offset(cx, cy),
                            alpha = 0.22f
                        )

                        // Refraction ring
                        drawCircle(
                            color = Color(0xFF93C5FD),
                            radius = r - 1.5f,
                            center = Offset(cx - r * 0.04f, cy - r * 0.04f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f),
                            alpha = 0.48f
                        )

                        // Top-left highlight reflection
                        drawCircle(
                            color = Color.White,
                            radius = r * 0.26f,
                            center = Offset(cx - r * 0.35f, cy - r * 0.35f),
                            alpha = 0.78f
                        )
                    }
                }
                Color(0xFFFAF3E0) -> {
                    // --- PARCHMENT (LIGHT) TEXTURE ---
                    // Draw warm paper fibers/spots
                    for (i in 0..30) {
                        val cx = random.nextFloat() * size.width
                        val cy = random.nextFloat() * size.height
                        val rx = random.nextFloat() * 12f + 4f
                        val alpha = random.nextFloat() * 0.03f + 0.01f
                        drawCircle(
                            color = Color(0xFF8B7355),
                            radius = rx,
                            center = Offset(cx, cy),
                            alpha = alpha
                        )
                    }
                    
                    // Draw fine organic paper fiber lines
                    for (i in 0..40) {
                        val startX = random.nextFloat() * size.width
                        val startY = random.nextFloat() * size.height
                        val length = random.nextFloat() * 30f + 10f
                        val angle = random.nextFloat() * 2 * Math.PI
                        val endX = startX + length * Math.cos(angle).toFloat()
                        val endY = startY + length * Math.sin(angle).toFloat()
                        val alpha = random.nextFloat() * 0.06f
                        drawLine(
                            color = Color(0xFF4A3B32),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 0.5f,
                            alpha = alpha
                        )
                    }
                }
                else -> {
                    // --- CHALKBOARD (DARK) TEXTURE ---
                    // Draw chalkboard scratches
                    for (i in 0..25) {
                        val startX = random.nextFloat() * size.width
                        val startY = random.nextFloat() * size.height
                        val length = random.nextFloat() * 80f + 20f
                        val angle = random.nextFloat() * 2 * Math.PI
                        val endX = startX + length * Math.cos(angle).toFloat()
                        val endY = startY + length * Math.sin(angle).toFloat()
                        val alpha = random.nextFloat() * 0.08f + 0.02f
                        drawLine(
                            color = Color(0xFFDDDDDD),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 0.75f,
                            alpha = alpha
                        )
                    }
                    
                    // Fine chalk dust particles
                    for (i in 0..250) {
                        val x = random.nextFloat() * size.width
                        val y = random.nextFloat() * size.height
                        val radius = random.nextFloat() * 1.5f + 0.5f
                        val alpha = random.nextFloat() * 0.20f
                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = Offset(x, y),
                            alpha = alpha
                        )
                    }
                }
            }
        }
        content()
    }
}

@Composable
fun ParchmentBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    ChalkboardBackground(modifier = modifier, content = content)
}

@Composable
fun DashedHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = ParchmentLine
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = pathEffect
        )
    }
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = ParchmentLine
) {
    DashedHorizontalDivider(modifier, thickness, color)
}
