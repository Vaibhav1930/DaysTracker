package com.vaibhav.daystracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TopColors = listOf(Color(0xFF3A3C40), Color(0xFF2E3033))
private val BottomColors = listOf(Color(0xFF26272A), Color(0xFF1C1D1F))
private val CardShape = RoundedCornerShape(18.dp)
private val Seam = Color(0xFF0C0C0D)

private fun Int.two() = toString().padStart(2, '0')

@Composable
fun FlipClock(
    hours: Long,
    minutes: Long,
    seconds: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlipUnit("HOURS", hours.toInt(), Modifier.weight(1f))
        FlipUnit("MIN", minutes.toInt(), Modifier.weight(1f))
        FlipUnit("SEC", seconds.toInt(), Modifier.weight(1f))
    }
}

@Composable
private fun FlipUnit(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FlipCard(value)
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color(0xFF8C8E93),
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun FlipCard(
    value: Int,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 72.dp,
    fontSize: TextUnit = 34.sp
) {
    var shown by remember { mutableIntStateOf(value) }
    var previous by remember { mutableIntStateOf(value) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(value) {
        if (value != shown) {
            previous = shown
            progress.snapTo(0f)
            shown = value
            progress.animateTo(1f, tween(450, easing = LinearEasing))
        }
    }

    val p = progress.value
    val newText = shown.two()
    val oldText = previous.two()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(8.dp, CardShape)
    ) {
        Column {
            Half(newText, top = true, cardHeight = cardHeight, fontSize = fontSize)
            Half(
                text = if (p >= 1f) newText else oldText,
                top = false,
                cardHeight = cardHeight,
                fontSize = fontSize
            )
        }

        if (p <= 0.5f && p < 1f) {
            Half(
                text = oldText,
                top = true,
                cardHeight = cardHeight,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        scaleY = 1f - progress.value * 2f
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
            )
        } else if (p < 1f) {
            Half(
                text = newText,
                top = false,
                cardHeight = cardHeight,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        scaleY = (progress.value - 0.5f) * 2f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
            )
        }

        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(2.dp)
                .background(Seam)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(width = 5.dp, height = 14.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(Seam)
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(width = 5.dp, height = 14.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                .background(Seam)
        )
    }
}

@Composable
private fun Half(
    text: String,
    top: Boolean,
    cardHeight: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val shape = if (top) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    } else {
        RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight / 2)
            .clip(shape)
            .background(Brush.verticalGradient(if (top) TopColors else BottomColors))
    ) {
        Box(
            modifier = Modifier
                .wrapContentHeight(
                    align = if (top) Alignment.Top else Alignment.Bottom,
                    unbounded = true
                )
                .fillMaxWidth()
                .height(cardHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (top) Color(0xFFF5F5F6) else Color(0xFFE4E4E6),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}