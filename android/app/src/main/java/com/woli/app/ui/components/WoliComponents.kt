package com.woli.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliWarning
import com.woli.app.ui.theme.WoliYellow

enum class EyeMood {
    Idle,
    Happy,
    Alert,
    Angry,
    Complete,
}

@Composable
fun WoliPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WoliYellow,
            contentColor = WoliBlack,
            disabledContainerColor = WoliYellow.copy(alpha = 0.4f),
            disabledContentColor = WoliBlack.copy(alpha = 0.5f),
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun WoliSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2C2C2E),
            contentColor = WoliText,
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun WoliEyes(
    mood: EyeMood,
    modifier: Modifier = Modifier,
    eyeSize: Dp = 72.dp,
    gap: Dp = 56.dp,
    showWarningBadge: Boolean = false,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyeGlyph(mood = mood, size = eyeSize)
            EyeGlyph(mood = mood, size = eyeSize)
        }
        if (showWarningBadge) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(36.dp),
            ) {
                val path = Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = WoliWarning)
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(size.width / 2f, size.height * 0.62f),
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width / 2f, size.height * 0.28f),
                    end = Offset(size.width / 2f, size.height * 0.48f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun EyeGlyph(mood: EyeMood, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = (size.toPx() * 0.12f)
        when (mood) {
            EyeMood.Idle, EyeMood.Alert -> {
                drawCircle(color = WoliCyan)
                drawCircle(
                    color = WoliBlack,
                    radius = size.toPx() * 0.28f,
                    center = center,
                )
            }
            EyeMood.Happy, EyeMood.Complete -> {
                val path = Path().apply {
                    val w = size.toPx()
                    moveTo(w * 0.1f, w * 0.55f)
                    quadraticTo(w * 0.5f, w * 0.15f, w * 0.9f, w * 0.55f)
                }
                drawPath(
                    path = path,
                    color = WoliCyan,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            EyeMood.Angry -> {
                val path = Path().apply {
                    val w = size.toPx()
                    moveTo(w * 0.08f, w * 0.28f)
                    lineTo(w * 0.92f, w * 0.48f)
                    quadraticTo(w * 0.5f, w * 0.95f, w * 0.08f, w * 0.55f)
                    close()
                }
                drawPath(path, color = WoliCyan)
            }
        }
    }
}

@Composable
fun WoliRobotMascot(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(width = 220.dp, height = 200.dp),
    ) {
        val bodyYellow = WoliYellow
        val screen = Color(0xFF111111)
        // body
        drawRoundRect(
            color = bodyYellow,
            topLeft = Offset(size.width * 0.18f, size.height * 0.42f),
            size = Size(size.width * 0.64f, size.height * 0.38f),
            cornerRadius = CornerRadius(18f, 18f),
        )
        // head / phone mount
        drawRoundRect(
            color = bodyYellow,
            topLeft = Offset(size.width * 0.22f, size.height * 0.08f),
            size = Size(size.width * 0.56f, size.height * 0.36f),
            cornerRadius = CornerRadius(16f, 16f),
        )
        drawRoundRect(
            color = screen,
            topLeft = Offset(size.width * 0.28f, size.height * 0.14f),
            size = Size(size.width * 0.44f, size.height * 0.24f),
            cornerRadius = CornerRadius(10f, 10f),
        )
        // eyes
        drawCircle(
            color = WoliCyan,
            radius = size.minDimension * 0.045f,
            center = Offset(size.width * 0.40f, size.height * 0.26f),
        )
        drawCircle(
            color = WoliCyan,
            radius = size.minDimension * 0.045f,
            center = Offset(size.width * 0.60f, size.height * 0.26f),
        )
        // treads
        drawRoundRect(
            color = Color(0xFF333333),
            topLeft = Offset(size.width * 0.12f, size.height * 0.78f),
            size = Size(size.width * 0.30f, size.height * 0.14f),
            cornerRadius = CornerRadius(20f, 20f),
        )
        drawRoundRect(
            color = Color(0xFF333333),
            topLeft = Offset(size.width * 0.58f, size.height * 0.78f),
            size = Size(size.width * 0.30f, size.height * 0.14f),
            cornerRadius = CornerRadius(20f, 20f),
        )
        // arms
        drawRoundRect(
            color = bodyYellow,
            topLeft = Offset(size.width * 0.02f, size.height * 0.48f),
            size = Size(size.width * 0.14f, size.height * 0.10f),
            cornerRadius = CornerRadius(8f, 8f),
        )
        drawRoundRect(
            color = bodyYellow,
            topLeft = Offset(size.width * 0.84f, size.height * 0.48f),
            size = Size(size.width * 0.14f, size.height * 0.10f),
            cornerRadius = CornerRadius(8f, 8f),
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, color = WoliMuted, fontSize = 13.sp)
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    color = WoliYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = WoliText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ShellHintBar(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = text, color = WoliMuted, fontSize = 12.sp)
    }
}
