package com.woli.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.ui.theme.WoliAnger
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliYellow
import kotlinx.coroutines.delay

private const val EYE_BLINK_INTERVAL_MS = 4_000L
private const val EYE_BLINK_DURATION_MS = 350L
private const val EYE_GAP_RATIO = 0.79f

data class FocusEyeMetrics(
    val eyeSize: Dp,
    val gap: Dp,
    val stageHeight: Dp,
)

object FocusEyeLayout {
    /** 기본 집중 화면 하단 칩 영역(14dp spacer + 칩 행) */
    val defaultBottomChrome: Dp = 62.dp
    const val fillFraction: Float = 0.7f
}

@Composable
fun rememberFocusEyeMetrics(
    bottomChrome: Dp = FocusEyeLayout.defaultBottomChrome,
    fillFraction: Float = FocusEyeLayout.fillFraction,
): FocusEyeMetrics {
    val configuration = LocalConfiguration.current
    val boundsWidth = configuration.screenWidthDp.dp - 48.dp
    val boundsHeight = (configuration.screenHeightDp.dp - 48.dp - bottomChrome).coerceAtLeast(0.dp)
    val eyeFromWidth = boundsWidth * fillFraction / (2f + EYE_GAP_RATIO)
    val eyeFromHeight = boundsHeight * fillFraction
    val eyeSize = minOf(eyeFromWidth, eyeFromHeight)
    return FocusEyeMetrics(
        eyeSize = eyeSize,
        gap = eyeSize * EYE_GAP_RATIO,
        stageHeight = boundsHeight,
    )
}

enum class EyeMood {
    Idle,
    Happy,
    Alert,
    Angry,
    Complete,
}

@Composable
fun WoliVoiceMicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    contentDescription: String = "음성 명령",
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .background(
                color = if (isListening) WoliYellow.copy(alpha = 0.75f) else WoliYellow,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = contentDescription,
            tint = WoliCyan,
            modifier = Modifier.size(32.dp),
        )
    }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(modifier),
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
    fillFraction: Float = FocusEyeLayout.fillFraction,
    eyeSize: Dp? = null,
    gap: Dp? = null,
    showWarningBadge: Boolean = false,
    mirrorAngry: Boolean = true,
) {
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(EYE_BLINK_INTERVAL_MS)
            isBlinking = true
            delay(EYE_BLINK_DURATION_MS)
            isBlinking = false
        }
    }

    val configuration = LocalConfiguration.current
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val resolvedEyeSize: Dp
        val resolvedGap: Dp
        if (eyeSize != null && gap != null) {
            resolvedEyeSize = eyeSize
            resolvedGap = gap
        } else {
            val boundsWidth = if (maxWidth != Dp.Infinity && maxWidth > 0.dp) {
                maxWidth
            } else {
                configuration.screenWidthDp.dp - 48.dp
            }
            val boundsHeight = if (maxHeight != Dp.Infinity && maxHeight > 0.dp) {
                maxHeight
            } else {
                configuration.screenHeightDp.dp - 48.dp
            }
            val eyeFromWidth = boundsWidth * fillFraction / (2f + EYE_GAP_RATIO)
            val eyeFromHeight = boundsHeight * fillFraction
            resolvedEyeSize = minOf(eyeFromWidth, eyeFromHeight)
            resolvedGap = resolvedEyeSize * EYE_GAP_RATIO
        }

        Box(contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(resolvedGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EyeGlyph(
                    mood = mood,
                    size = resolvedEyeSize,
                    flipHorizontal = false,
                    isBlinking = isBlinking,
                )
                EyeGlyph(
                    mood = mood,
                    size = resolvedEyeSize,
                    flipHorizontal = mirrorAngry && mood == EyeMood.Angry,
                    isBlinking = isBlinking,
                )
            }
            if (showWarningBadge) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(resolvedEyeSize * 0.76f),
                ) {
                    drawAngerVeinMark(color = WoliAnger)
                }
            }
        }
    }
}

@Composable
private fun EyeGlyph(
    mood: EyeMood,
    size: Dp,
    flipHorizontal: Boolean = false,
    isBlinking: Boolean = false,
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val cell = (w * 0.075f).coerceIn(4f, 14f)
        val blinkColor = if (mood == EyeMood.Angry) WoliAnger else WoliCyan
        if (isBlinking) {
            drawPixelBlinkLine(center = center, halfWidth = w * 0.42f, color = blinkColor, cell = cell)
            return@Canvas
        }
        when (mood) {
            // 기획 시나리오: LED 도트 원형 눈
            EyeMood.Idle, EyeMood.Alert -> {
                drawPixelCircle(center = center, radius = w * 0.48f, color = WoliCyan, cell = cell)
            }
            // 놀람/완료: 위쪽 아치 (n n) — 동일 도트
            EyeMood.Happy, EyeMood.Complete -> {
                drawPixelArc(
                    p0 = Offset(w * 0.06f, w * 0.62f),
                    p1 = Offset(w * 0.5f, w * 0.08f),
                    p2 = Offset(w * 0.94f, w * 0.62f),
                    stroke = w * 0.18f,
                    color = WoliCyan,
                    cell = cell,
                )
            }
            // 손 접근: Idle과 같은 크기·위치의 원을 비스듬히 반으로 자른 채움 눈
            EyeMood.Angry -> {
                // 왼쪽 \, 오른쪽 / — 평평한 컷이 가운데(안쪽)로 기울어 화난 인상
                val diameterDeg = if (!flipHorizontal) 38f else -38f
                drawPixelHalfCircle(
                    center = center,
                    radius = w * 0.48f,
                    diameterAngleDeg = diameterDeg,
                    keepBelow = true,
                    color = WoliAnger,
                    cell = cell,
                )
            }
        }
    }
}

/**
 * 만화 💢 — 네 모서리 원호(끝은 각지게).
 * 작은 캔버스+두꺼운 스트로크면 뭉개지므로 비율을 넉넉히 둔다.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAngerVeinMark(
    color: Color,
) {
    val s = size.minDimension
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val radius = s * 0.34f
    val sweep = 58f
    val stroke = Stroke(
        width = s * 0.168f,
        cap = StrokeCap.Butt,
        join = StrokeJoin.Miter,
    )
    // drawArc: 0°=3시, 시계방향. 모서리 중앙각 NW/NE/SE/SW
    val midAngles = floatArrayOf(225f, 315f, 45f, 135f)
    val topLeft = Offset(cx - radius, cy - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    for (mid in midAngles) {
        drawArc(
            color = color,
            startAngle = mid - sweep * 0.5f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

/** Idle 원과 동일 반경·중심, 지름선으로 반만 채움(비스듬한 화난 눈) */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelHalfCircle(
    center: Offset,
    radius: Float,
    diameterAngleDeg: Float,
    keepBelow: Boolean,
    color: Color,
    cell: Float,
) {
    val angle = Math.toRadians(diameterAngleDeg.toDouble())
    // 지름선에 수직인 법선 — keepBelow면 법선 아래쪽(화면 +y) 반을 유지
    val nx = (-kotlin.math.sin(angle)).toFloat()
    val ny = kotlin.math.cos(angle).toFloat()
    val side = if (keepBelow) 1f else -1f
    val r2 = radius * radius
    var y = center.y - radius
    while (y <= center.y + radius) {
        var x = center.x - radius
        while (x <= center.x + radius) {
            val px = x + cell * 0.5f
            val py = y + cell * 0.5f
            val dx = px - center.x
            val dy = py - center.y
            if (dx * dx + dy * dy <= r2 && (dx * nx + dy * ny) * side >= 0f) {
                drawPixelDot(x, y, cell, color)
            }
            x += cell
        }
        y += cell
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelBlinkLine(
    center: Offset,
    halfWidth: Float,
    color: Color,
    cell: Float,
) {
    var x = center.x - halfWidth
    while (x <= center.x + halfWidth) {
        drawPixelDot(x, center.y - cell * 0.5f, cell, color)
        x += cell
    }
}

/** ponytail: LED 도트 눈 — 셀 그리드 O(n²). 고해상도면 Bitmap 캐시로 교체 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelDot(
    x: Float,
    y: Float,
    cell: Float,
    color: Color,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(cell * 0.86f, cell * 0.86f),
        cornerRadius = CornerRadius(cell * 0.18f, cell * 0.18f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelCircle(
    center: Offset,
    radius: Float,
    color: Color,
    cell: Float = (radius * 0.16f).coerceIn(4f, 14f),
) {
    val r2 = radius * radius
    var y = center.y - radius
    while (y <= center.y + radius) {
        var x = center.x - radius
        while (x <= center.x + radius) {
            val dx = x + cell * 0.5f - center.x
            val dy = y + cell * 0.5f - center.y
            if (dx * dx + dy * dy <= r2) drawPixelDot(x, y, cell, color)
            x += cell
        }
        y += cell
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelArc(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    stroke: Float,
    color: Color,
    cell: Float,
) {
    val half = stroke * 0.5f
    val minX = minOf(p0.x, p1.x, p2.x) - half
    val maxX = maxOf(p0.x, p1.x, p2.x) + half
    val minY = minOf(p0.y, p1.y, p2.y) - half
    val maxY = maxOf(p0.y, p1.y, p2.y) + half
    val samples = 48
    val curve = Array(samples + 1) { i ->
        val t = i / samples.toFloat()
        val u = 1f - t
        Offset(
            u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x,
            u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y,
        )
    }
    val half2 = half * half
    var y = minY
    while (y <= maxY) {
        var x = minX
        while (x <= maxX) {
            val cx = x + cell * 0.5f
            val cy = y + cell * 0.5f
            var near = false
            for (p in curve) {
                val dx = cx - p.x
                val dy = cy - p.y
                if (dx * dx + dy * dy <= half2) {
                    near = true
                    break
                }
            }
            if (near) drawPixelDot(x, y, cell, color)
            x += cell
        }
        y += cell
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
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
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
                    modifier = Modifier.clickable(onClick = onAction),
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
