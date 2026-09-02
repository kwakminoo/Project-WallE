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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
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
import com.woli.app.focus.WoliFocusNotificationPermissions
import com.woli.app.focus.WoliFocusSessionDemos
import com.woli.app.focus.WoliFocusSessionController
import com.woli.app.focus.dailyFocusStats
import com.woli.app.focus.formatDurationKorean
import com.woli.app.focus.isEarlyUnlock
import com.woli.app.ui.components.StatCard
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.components.WoliRobotMascot
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliOrange
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliWarning
import com.woli.app.ui.theme.WoliYellow

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onOpenStats: () -> Unit,
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
            .background(WoliBlack),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
        }
        BottomNavBar(
            selected = NavTab.Home,
            onHome = {},
            onStats = onOpenStats,
            onSettings = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            docked = true,
        )
    }
}

enum class NavTab { Home, Stats, Settings }

@Composable
fun BottomNavBar(
    selected: NavTab,
    onHome: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    shape: Shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    applyNavigationBarsPadding: Boolean = true,
    docked: Boolean = false,
) {
    val barColor = Color(0xFF141414)
    val dockedShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )

    if (docked) {
        Column(
            modifier = modifier.background(color = barColor, shape = dockedShape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                NavItem("홈", Icons.Default.Home, selected == NavTab.Home, onHome)
                NavItem("통계", Icons.Default.BarChart, selected == NavTab.Stats, onStats)
                NavItem("설정", Icons.Default.Settings, selected == NavTab.Settings, onSettings)
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            )
        }
        return
    }

    Row(
        modifier = modifier
            .background(
                color = barColor,
                shape = shape,
            )
            .then(
                if (applyNavigationBarsPadding) {
                    Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                } else {
                    Modifier
                },
            )
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavItem("홈", Icons.Default.Home, selected == NavTab.Home, onHome)
        NavItem("통계", Icons.Default.BarChart, selected == NavTab.Stats, onStats)
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
fun StatsScreen(onBackHome: () -> Unit, onSettings: () -> Unit) {
    val focusHistory by WoliFocusSessionController.history.collectAsState()
    val statsHistory = remember(focusHistory) {
        focusHistory.ifEmpty { WoliFocusSessionDemos.sampleHistory() }
    }
    val dailyStats = remember(statsHistory) { statsHistory.dailyFocusStats() }

    val totalFocusMillis = statsHistory.sumOf { it.focusedElapsedMillis() }
    val handWarnings = statsHistory.sumOf { it.handWarningCount }
    val earlyUnlocks = statsHistory.count { it.exitReason.isEarlyUnlock() }

    ShellTabScaffold(
        title = "집중 통계",
        subtitle = "최근 7일 기록",
        selected = NavTab.Stats,
        onHome = onBackHome,
        onStats = {},
        onSettings = onSettings,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatsSummaryChip(
                title = "누적 집중",
                value = totalFocusMillis.formatDurationKorean(),
                color = WoliYellow,
                modifier = Modifier.weight(1f),
            )
            StatsSummaryChip(
                title = "손 접근 경고",
                value = "${handWarnings}회",
                color = WoliOrange,
                modifier = Modifier.weight(1f),
            )
            StatsSummaryChip(
                title = "중도 해제",
                value = "${earlyUnlocks}회",
                color = WoliWarning,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        DailyBarChartSection(
            title = "누적 집중",
            color = WoliYellow,
            points = dailyStats.map { it.dayLabel to it.focusMinutes },
            formatValue = { minutes -> if (minutes >= 60) "${minutes / 60}h" else "${minutes}m" },
        )
        Spacer(modifier = Modifier.height(22.dp))
        DailyBarChartSection(
            title = "손 접근 경고",
            color = WoliOrange,
            points = dailyStats.map { it.dayLabel to it.handWarnings },
            formatValue = { count -> "${count}회" },
        )
        Spacer(modifier = Modifier.height(22.dp))
        DailyBarChartSection(
            title = "중도 해제",
            color = WoliWarning,
            points = dailyStats.map { it.dayLabel to it.earlyUnlocks },
            formatValue = { count -> "${count}회" },
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatsSummaryChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = WoliMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = WoliText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun DailyBarChartSection(
    title: String,
    color: Color,
    points: List<Pair<String, Int>>,
    formatValue: (Int) -> String,
) {
    val maxValue = points.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(text = title, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEach { (label, value) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (value > 0) {
                        Text(
                            text = formatValue(value),
                            color = color,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val barHeight = if (value > 0) {
                        (value.toFloat() / maxValue * 88f).coerceAtLeast(6f)
                    } else {
                        4f
                    }
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(barHeight.dp)
                            .background(
                                color.copy(alpha = if (value > 0) 0.92f else 0.18f),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = label,
                        color = WoliMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBackHome: () -> Unit,
    onStats: () -> Unit,
    onOpenPermissionSetup: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenNotificationPolicy: () -> Unit,
    onOpenNotificationDiagnostics: () -> Unit,
    onOpenHardwareDiagnostics: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    ShellTabScaffold(
        title = "설정",
        subtitle = "집중 세션과 알림/전화 연동 상태",
        selected = NavTab.Settings,
        onHome = onBackHome,
        onStats = onStats,
        onSettings = {},
    ) {
        SettingsRow("권한 설정", onOpenPermissionSetup)
        SettingsRow("블루투스 연결", onOpenBluetoothSettings)
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
    subtitle: String,
    selected: NavTab,
    onHome: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
        Spacer(modifier = Modifier.height(28.dp))
        Text(text = title, color = WoliText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
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
        }
        BottomNavBar(
            selected = selected,
            onHome = onHome,
            onStats = onStats,
            onSettings = onSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            docked = true,
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
