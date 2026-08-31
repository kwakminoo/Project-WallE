package com.woli.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.call.WoliCallCenter
import com.woli.app.focus.WoliFocusNotificationPermissions
import com.woli.app.focus.WoliFocusSessionController
import com.woli.app.focus.formatDurationKorean
import com.woli.app.notification.WoliReplyHistoryResultType
import com.woli.app.notification.WoliReplyHistoryStore
import com.woli.app.ui.components.StatCard
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliRobotMascot
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliYellow
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissionSetup: () -> Unit,
) {
    val context = LocalContext.current
    var showPermissionPrompt by remember { mutableStateOf(false) }
    val config by WoliFocusSessionController.config.collectAsState()
    val focusHistory by WoliFocusSessionController.history.collectAsState()
    val totalFocusMillis = focusHistory.sumOf { session ->
        session.elapsedMillis(session.completedAtMillis ?: System.currentTimeMillis())
    }

    LaunchedEffect(Unit) {
        if (!WoliFocusNotificationPermissions.allGranted(context)) {
            showPermissionPrompt = true
        }
    }

    if (showPermissionPrompt) {
        AlertDialog(
            onDismissRequest = { showPermissionPrompt = false },
            title = {
                Text(
                    text = "권한 설정이 필요해요",
                    color = WoliText,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "중요 알림과 전화를 월이가 전달하려면 권한 설정이 필요합니다. 지금 설정할까요?",
                    color = WoliMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionPrompt = false
                        onOpenPermissionSetup()
                    },
                ) {
                    Text("권한 설정하기", color = WoliYellow, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionPrompt = false }) {
                    Text("나중에", color = WoliMuted)
                }
            },
            containerColor = Color(0xFF1C1C1E),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "안녕! 나는 월이야.\n오늘도 함께 집중해볼까?",
            color = WoliText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 30.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            WoliRobotMascot()
        }
        StatCard(
            title = "오늘의 집중 목표",
            value = "${config.durationMinutes}분",
            actionLabel = "수정",
            onAction = onStartFocus,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                title = "연속 집중",
                value = "${focusHistory.size}회 완료",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "누적 집중",
                value = totalFocusMillis.formatDurationKorean(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        WoliPrimaryButton(text = "집중 시작하기", onClick = onStartFocus)
        Spacer(modifier = Modifier.height(16.dp))
        BottomNavBar(
            selected = NavTab.Home,
            onHome = {},
            onStats = onOpenStats,
            onMissions = onOpenMissions,
            onSettings = onOpenSettings,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

enum class NavTab { Home, Stats, Missions, Settings }

@Composable
fun BottomNavBar(
    selected: NavTab,
    onHome: () -> Unit,
    onStats: () -> Unit,
    onMissions: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414), RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavItem("홈", Icons.Default.Home, selected == NavTab.Home, onHome)
        NavItem("통계", Icons.Default.BarChart, selected == NavTab.Stats, onStats)
        NavItem("미션", Icons.Default.Extension, selected == NavTab.Missions, onMissions)
        NavItem("설정", Icons.Default.Settings, selected == NavTab.Settings, onSettings)
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (selected) WoliYellow.copy(alpha = 0.18f) else Color.Transparent,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) WoliYellow else WoliMuted,
            )
        }
        Text(
            text = label,
            color = if (selected) WoliYellow else WoliMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
fun StatsScreen(onBackHome: () -> Unit, onMissions: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val focusHistory by WoliFocusSessionController.history.collectAsState()
    val callHistory by WoliCallCenter.history.collectAsState()
    val replyHistory by WoliReplyHistoryStore.entries.collectAsState()

    LaunchedEffect(context) {
        WoliReplyHistoryStore.load(context)
    }

    val totalFocusMillis = focusHistory.sumOf { session ->
        session.elapsedMillis(session.completedAtMillis ?: System.currentTimeMillis())
    }
    val handWarnings = focusHistory.sumOf { it.handWarningCount }
    val sentReplies = replyHistory.count { it.resultType == WoliReplyHistoryResultType.Sent }

    ShellTabScaffold(
        title = "집중 통계",
        selected = NavTab.Stats,
        onHome = onBackHome,
        onStats = {},
        onMissions = onMissions,
        onSettings = onSettings,
    ) {
        StatCard(title = "누적 집중", value = totalFocusMillis.formatDurationKorean())
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "손 접근 경고", value = "${handWarnings}회")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "전화 감지", value = "${callHistory.size}회")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "답장 성공", value = "${sentReplies}회")
    }
}

@Composable
fun MissionsScreen(
    onBackHome: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onOpenBreathing: () -> Unit,
    onOpenMemory: () -> Unit,
) {
    ShellTabScaffold(
        title = "미션",
        selected = NavTab.Missions,
        onHome = onBackHome,
        onStats = onStats,
        onMissions = {},
        onSettings = onSettings,
    ) {
        StatCard(title = "리듬 미션", value = "중도 해제 시 실행")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(
            title = "호흡 미션",
            value = "30초 호흡으로 충동 멈춤",
            actionLabel = "시작",
            onAction = onOpenBreathing,
        )
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(
            title = "기억력 미션",
            value = "간단한 패턴 기억하기",
            actionLabel = "시작",
            onAction = onOpenMemory,
        )
    }
}

@Composable
fun SettingsScreen(
    onBackHome: () -> Unit,
    onStats: () -> Unit,
    onMissions: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenNotificationPolicy: () -> Unit,
    onOpenNotificationDiagnostics: () -> Unit,
    onOpenHardwareDiagnostics: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    ShellTabScaffold(
        title = "설정",
        selected = NavTab.Settings,
        onHome = onBackHome,
        onStats = onStats,
        onMissions = onMissions,
        onSettings = {},
    ) {
        SettingsRow("월이 기기 연결", onOpenDevice)
        SettingsRow("중요 연락처", onOpenContacts)
        SettingsRow("집중 알림 기준", onOpenNotificationPolicy)
        SettingsRow("화면 상태 갤러리", onOpenGallery)
        SettingsRow("알림 / TTS 검증", onOpenNotificationDiagnostics)
        SettingsRow("하드웨어 검증", onOpenHardwareDiagnostics)
        SettingsRow("앱 정보", onOpenAppInfo)
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(text = label, color = WoliText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ShellTabScaffold(
    title: String,
    selected: NavTab,
    onHome: () -> Unit,
    onStats: () -> Unit,
    onMissions: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Text(text = title, color = WoliText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "집중 세션과 알림/전화 연동 상태",
            color = WoliMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
        BottomNavBar(
            selected = selected,
            onHome = onHome,
            onStats = onStats,
            onMissions = onMissions,
            onSettings = onSettings,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun BreathingMissionScreen(onBack: () -> Unit) {
    var remainingSeconds by remember { mutableIntStateOf(30) }
    var running by remember { mutableStateOf(false) }
    val elapsed = 30 - remainingSeconds
    val phase = when ((elapsed / 4) % 3) {
        0 -> "들이마시기"
        1 -> "멈추기"
        else -> "내쉬기"
    }

    LaunchedEffect(running) {
        while (running && remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds -= 1
        }
        if (remainingSeconds <= 0) running = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackTitle(title = "호흡 미션", onBack = onBack)
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .size(176.dp)
                .background(WoliYellow.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = phase,
                color = WoliYellow,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "${remainingSeconds}초",
            color = WoliText,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (remainingSeconds == 0) "충동을 멈추는 미션을 완료했습니다." else "잠금 해제 전 호흡을 안정시키는 미션입니다.",
            color = WoliMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
        WoliPrimaryButton(
            text = if (running) "진행 중" else if (remainingSeconds == 0) "다시 시작" else "시작",
            onClick = {
                if (remainingSeconds == 0) remainingSeconds = 30
                running = true
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        WoliSecondaryButton(
            text = "초기화",
            onClick = {
                running = false
                remainingSeconds = 30
            },
        )
    }
}

@Composable
fun MemoryMissionScreen(onBack: () -> Unit) {
    val pattern = remember { listOf("노랑", "파랑", "노랑", "초록") }
    var input by remember { mutableStateOf(emptyList<String>()) }
    var message by remember { mutableStateOf("패턴을 보고 같은 순서로 입력하세요.") }
    val palette = listOf("노랑", "파랑", "초록")

    fun reset() {
        input = emptyList()
        message = "패턴을 보고 같은 순서로 입력하세요."
    }

    fun submit(value: String) {
        val next = input + value
        input = next
        if (pattern.take(next.size) != next) {
            message = "순서가 달라졌습니다. 다시 시도하세요."
            input = emptyList()
            return
        }
        if (next.size == pattern.size) {
            message = "기억력 미션을 완료했습니다."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "기억력 미션", onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        Text("패턴", color = WoliMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pattern.forEach { label ->
                MissionToken(label = label, active = true, onClick = {})
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text("입력", color = WoliMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pattern.indices.forEach { index ->
                MissionToken(label = input.getOrNull(index) ?: "-", active = input.getOrNull(index) != null, onClick = {})
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(message, color = WoliText, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        palette.forEach { label ->
            WoliSecondaryButton(text = label, onClick = { submit(label) })
            Spacer(modifier = Modifier.height(8.dp))
        }
        WoliPrimaryButton(text = "다시 시작", onClick = ::reset)
    }
}

@Composable
private fun MissionToken(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 46.dp)
            .background(
                if (active) WoliYellow.copy(alpha = 0.22f) else Color(0xFF1C1C1E),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) WoliYellow else WoliMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun AppInfoScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "앱 정보", onBack = onBack)
        Spacer(modifier = Modifier.height(18.dp))
        StatCard(title = "버전", value = "0.1.0")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "핵심 기능", value = "집중 잠금 · 중요 알림 · 음성 답장")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "기기 연동", value = "BLE WOLI GATT")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "개인정보", value = "연락처/답장 이력은 기기 내부 저장")
        Spacer(modifier = Modifier.weight(1f))
        WoliSecondaryButton(text = "뒤로", onClick = onBack)
    }
}

@Composable
fun PlaceholderCenter(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = WoliMuted, textAlign = TextAlign.Center)
    }
}
