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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.ui.components.StatCard
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliRobotMascot
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliYellow

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenSettings: () -> Unit,
) {
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
            value = "2시간 30분",
            actionLabel = "수정",
            onAction = {},
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                title = "연속 집중",
                value = "5일",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "누적 집중",
                value = "12시간 45분",
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
    ShellTabScaffold(
        title = "집중 통계",
        selected = NavTab.Stats,
        onHome = onBackHome,
        onStats = {},
        onMissions = onMissions,
        onSettings = onSettings,
    ) {
        StatCard(title = "이번 주 집중", value = "8시간 20분")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "손 접근 경고", value = "14회")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "중요 연락 수신", value = "6회")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "월이 친밀도", value = "Lv.3 · 신뢰의 눈")
    }
}

@Composable
fun MissionsScreen(onBackHome: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit) {
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
        StatCard(title = "호흡 미션", value = "30초 호흡으로 충동 멈춤")
        Spacer(modifier = Modifier.height(10.dp))
        StatCard(title = "기억력 미션", value = "간단한 패턴 기억하기")
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
        SettingsRow("화면 껍데기 갤러리 (데모)", onOpenGallery)
        SettingsRow("알림 / TTS", {})
        SettingsRow("앱 정보", {})
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
            text = "UI 껍데기 — 기능 연결 전 미리보기",
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
fun PlaceholderCenter(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = WoliMuted, textAlign = TextAlign.Center)
    }
}
