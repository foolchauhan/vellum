package com.example.vellum.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
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
    val isDark = MaterialTheme.colorScheme.background == ChalkboardSlate
    Box(
        modifier = Modifier
            .background(if (isDark) Color(0xFF1E2322) else Color(0xFFFAF3E0))
            .then(modifier)
    ) {
        if (isDark) {
            Image(
                painter = painterResource(id = com.example.vellum.R.drawable.blackboard_background_01),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            val random = Random(42) // Seeded for consistency
            
            if (isDark) {
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
            } else {
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
