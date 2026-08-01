package com.woli.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.ui.components.EyeMood
import com.woli.app.ui.components.ShellHintBar
import com.woli.app.ui.components.WoliEyes
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliOrange
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliWarning
import com.woli.app.ui.theme.WoliYellow

@Composable
fun FocusEyesScreen(
    onShowRemaining: () -> Unit,
    onShowCall: () -> Unit,
    onShowWarning: () -> Unit,
    onQuit: () -> Unit,
    onComplete: () -> Unit,
) {
    LandscapeFocusScaffold {
        WoliEyes(mood = EyeMood.Idle, eyeSize = 88.dp, gap = 72.dp)
        Spacer(modifier = Modifier.height(28.dp))
        DemoChipRow(
            chips = listOf(
                "남은시간" to onShowRemaining,
                "중요연락" to onShowCall,
                "손접근" to onShowWarning,
                "완료" to onComplete,
                "해제" to onQuit,
            ),
        )
    }
}

@Composable
fun RemainingTimeScreen(onBackEyes: () -> Unit) {
    LandscapeFocusScaffold {
        Text(
            text = "남은 시간  00:20:00",
            color = WoliText,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(28.dp))
        WoliEyes(mood = EyeMood.Idle, eyeSize = 80.dp, gap = 64.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "10분 단위로 2~3초만 표시 후 사라집니다",
            color = WoliMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        DemoChip("눈 화면으로", onBackEyes)
    }
}

@Composable
fun ImportantCallScreen(
    onAnswer: () -> Unit,
    onLater: () -> Unit,
    onContinue: () -> Unit,
) {
    LandscapeFocusScaffold {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WoliEyes(mood = EyeMood.Happy, eyeSize = 70.dp, gap = 48.dp)
            Spacer(modifier = Modifier.width(20.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Call, contentDescription = null, tint = WoliYellow, modifier = Modifier.size(28.dp))
                Text("어머니", color = WoliText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("전화가 왔어요", color = WoliMuted, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "TTS: \"어머니에게 전화가 왔어요.\"",
            color = WoliCyan,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DemoChip("받기", onAnswer)
            DemoChip("나중에", onLater)
            DemoChip("집중 계속", onContinue)
        }
    }
}

@Composable
fun HandWarningScreen(onDismiss: () -> Unit) {
    LandscapeFocusScaffold {
        WoliEyes(mood = EyeMood.Angry, eyeSize = 88.dp, gap = 72.dp, showWarningBadge = true)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "아직 집중 시간이 남았어요",
            color = WoliWarning,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "TTS 안내 · 손이 멀어지면 기본 눈으로 복귀",
            color = WoliMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        DemoChip("손이 멀어짐", onDismiss)
    }
}

@Composable
fun FocusCompleteScreen(onReport: () -> Unit, onHome: () -> Unit) {
    LandscapeFocusScaffold {
        Box {
            ConfettiHints()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("집중 완료!", color = WoliYellow, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(18.dp))
                WoliEyes(mood = EyeMood.Complete, eyeSize = 80.dp, gap = 64.dp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            MiniStat("집중 시간", "01:30:00")
            MiniStat("경고", "2회")
            MiniStat("중요 연락", "1회")
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DemoChip("리포트", onReport)
            DemoChip("홈으로", onHome)
        }
    }
}

@Composable
fun QuitConfirmScreen(onContinue: () -> Unit, onStartMission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("정말 종료할까요?", color = WoliText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "잠금을 해제하려면 미션을 완료해야 해요.",
                color = WoliMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            WoliPrimaryButton(text = "집중 계속하기", onClick = onContinue)
            Spacer(modifier = Modifier.height(10.dp))
            WoliSecondaryButton(text = "미션 시작", onClick = onStartMission)
        }
    }
}

@Composable
fun RhythmMissionScreen(onSuccess: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("COMBO 245", color = WoliYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("PERFECT", color = WoliCyan, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("리듬 미션 · 10초 탭으로 해제", color = WoliMuted, fontSize = 13.sp)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            RhythmShellVisual()
        }
        Text("SCORE  12,480", color = WoliText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        ShellHintBar(text = "껍데기: 노트 판정/점수 로직은 아직 없습니다.")
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(0.7f)) {
            Box(modifier = Modifier.weight(1f)) { WoliSecondaryButton("취소", onCancel) }
            Box(modifier = Modifier.weight(1f)) { WoliPrimaryButton("미션 성공(데모)", onSuccess) }
        }
    }
}

@Composable
fun SessionReportScreen(onHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("세션 리포트", color = WoliText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReportCard("총 집중", "90분")
            ReportCard("중도 해제", "0회")
            ReportCard("손 접근", "2회")
            ReportCard("중요 연락", "1회")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReportCard("연속 집중", "6일")
            ReportCard("친밀도", "+12 XP")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.width(280.dp)) {
            WoliPrimaryButton(text = "홈으로 돌아가기", onClick = onHome)
        }
    }
}

@Composable
private fun LandscapeFocusScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}

@Composable
private fun DemoChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF2C2C2E), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = WoliText, fontSize = 13.sp)
    }
}

@Composable
private fun DemoChipRow(chips: List<Pair<String, () -> Unit>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { (label, onClick) -> DemoChip(label, onClick) }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WoliMuted, fontSize = 12.sp)
        Text(value, color = WoliText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReportCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = WoliMuted, fontSize = 12.sp)
        Text(value, color = WoliYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConfettiHints() {
    Canvas(modifier = Modifier.size(260.dp, 120.dp)) {
        val colors = listOf(WoliOrange, WoliYellow, WoliCyan, WoliWarning)
        listOf(
            Offset(40f, 20f), Offset(80f, 50f), Offset(140f, 10f),
            Offset(200f, 40f), Offset(240f, 15f), Offset(60f, 80f),
            Offset(180f, 70f), Offset(220f, 90f),
        ).forEachIndexed { i, o ->
            drawCircle(color = colors[i % colors.size], radius = 4f, center = o)
        }
    }
}

@Composable
private fun RhythmShellVisual() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .fillMaxHeight(0.85f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val origin = Offset(size.width / 2f, size.height * 0.18f)
            val targets = listOf(
                Offset(size.width * 0.2f, size.height * 0.88f),
                Offset(size.width * 0.5f, size.height * 0.88f),
                Offset(size.width * 0.8f, size.height * 0.88f),
            )
            val laneColors = listOf(Color(0xFFBF5AF2), Color(0xFF30D158), WoliYellow)
            targets.forEachIndexed { i, t ->
                drawLine(laneColors[i], origin, t, strokeWidth = 4f)
                drawCircle(laneColors[i].copy(alpha = 0.35f), radius = 28f, center = Offset(
                    origin.x + (t.x - origin.x) * 0.45f,
                    origin.y + (t.y - origin.y) * 0.45f,
                ))
                drawCircle(Color.White.copy(alpha = 0.2f), radius = 34f, center = t)
                drawCircle(laneColors[i], radius = 18f, center = t)
            }
            drawCircle(WoliCyan, radius = 16f, center = origin)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(2.dp, WoliMuted, CircleShape),
                )
            }
        }
    }
}
