package com.woli.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.ui.components.ShellHintBar
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliYellow

@Composable
fun FocusTimeSettingScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var hour by remember { mutableIntStateOf(1) }
    var minute by remember { mutableIntStateOf(30) }
    var allowImportantOnly by remember { mutableStateOf(true) }
    var breakNotify by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "집중 시간 설정", onBack = onBack)
        Text(
            text = "얼마나 집중할까요?",
            color = WoliMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(20.dp))
                .padding(vertical = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeWheel(value = hour, unit = "시간", onMinus = { if (hour > 0) hour-- }, onPlus = { if (hour < 5) hour++ })
            Text(":", color = WoliYellow, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            TimeWheel(value = minute, unit = "분", onMinus = { minute = (minute + 55) % 60 }, onPlus = { minute = (minute + 5) % 60 })
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(25, 50, 90, 120).forEach { m ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(20.dp))
                        .clickable {
                            hour = m / 60
                            minute = m % 60
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("${m}분", color = WoliText, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        OptionToggle(
            title = "집중 모드",
            subtitle = "중요 연락만 허용",
            checked = allowImportantOnly,
            onCheckedChange = { allowImportantOnly = it },
        )
        Spacer(modifier = Modifier.height(10.dp))
        OptionToggle(
            title = "휴식 알림",
            subtitle = if (breakNotify) "사용 중" else "사용 안 함",
            checked = breakNotify,
            onCheckedChange = { breakNotify = it },
        )
        Spacer(modifier = Modifier.weight(1f))
        ShellHintBar(text = "껍데기: 값은 화면에만 반영되며 BLE/타이머는 아직 연결되지 않습니다.")
        Spacer(modifier = Modifier.height(12.dp))
        WoliPrimaryButton(text = "다음", onClick = onNext)
    }
}

@Composable
private fun TimeWheel(
    value: Int,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("+", color = WoliYellow, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onPlus))
        Text(
            text = "%02d".format(value),
            color = WoliText,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(unit, color = WoliMuted, fontSize = 13.sp)
        Text("−", color = WoliYellow, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onMinus))
    }
}

@Composable
private fun OptionToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = WoliText, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = WoliMuted, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WoliBlack,
                checkedTrackColor = WoliYellow,
            ),
        )
    }
}

@Composable
fun DeviceConnectScreen(onBack: () -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "월이 기기 연결", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text("BLE로 월이 로봇을 연결하세요.", color = WoliMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        DeviceRow(name = "WOLI-DT01", connected = true)
        Spacer(modifier = Modifier.height(10.dp))
        DeviceRow(name = "WOLI-Prototype", connected = false)
        Spacer(modifier = Modifier.weight(1f))
        WoliSecondaryButton(text = "다시 검색", onClick = {})
        Spacer(modifier = Modifier.height(10.dp))
        WoliPrimaryButton(text = "다음", onClick = onNext)
    }
}

@Composable
private fun DeviceRow(name: String, connected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = WoliCyan)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = WoliText, fontWeight = FontWeight.SemiBold)
            Text(if (connected) "연결됨" else "사용 가능", color = WoliMuted, fontSize = 12.sp)
        }
        if (connected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF30D158))
        }
    }
}

@Composable
fun ImportantContactsScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val contacts = listOf("어머니", "아버지", "학교 담임", "직장 동료")
    var selected by remember { mutableStateOf(setOf("어머니", "아버지")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "중요 연락처", onBack = onBack)
        Text("집중 중에도 받을 연락을 선택하세요.", color = WoliMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(20.dp))
        contacts.forEach { name ->
            val on = name in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
                    .clickable {
                        selected = if (on) selected - name else selected + name
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (on) WoliYellow else Color(0xFF2C2C2E), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(name.take(1), color = if (on) WoliBlack else WoliText, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(name, color = WoliText, modifier = Modifier.weight(1f))
                Text(if (on) "허용" else "차단", color = if (on) WoliCyan else WoliMuted, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        WoliPrimaryButton(text = "다음", onClick = onNext)
    }
}

@Composable
fun MountGuideScreen(onBack: () -> Unit, onStartFocus: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackTitle(title = "스마트폰 거치", onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(160.dp)
                .border(2.dp, WoliYellow, RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1E), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = WoliCyan,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "스마트폰을 가로로\n월이 머리 거치대에 올려주세요",
            color = WoliText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "거치가 확인되면 잠금이 작동하고\n집중 모드(눈 화면)로 전환됩니다.",
            color = WoliMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        ShellHintBar(text = "껍데기: 거치 감지/서보 잠금은 아직 미연결입니다.")
        Spacer(modifier = Modifier.height(12.dp))
        WoliPrimaryButton(text = "집중 모드 미리보기", onClick = onStartFocus)
    }
}

@Composable
fun BackTitle(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "뒤로",
            tint = WoliText,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 12.dp),
        )
        Text(text = title, color = WoliText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ShellGalleryScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val items = listOf(
        "home" to "홈",
        "focus_time" to "집중 시간 설정",
        "device_connect" to "기기 연결",
        "important_contacts" to "중요 연락처",
        "mount_guide" to "거치 안내",
        "focus_eyes" to "집중 눈 화면",
        "remaining_time" to "남은 시간 표시",
        "important_call" to "중요 연락",
        "hand_warning" to "손 접근 경고",
        "focus_complete" to "집중 완료",
        "quit_confirm" to "중도 해제 확인",
        "rhythm_mission" to "리듬 미션",
        "session_report" to "세션 리포트",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "화면 껍데기 갤러리", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text("SW 예상 시나리오 화면을 개별 확인합니다.", color = WoliMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            items.forEach { (route, label) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                        .clickable { onOpen(route) }
                        .padding(16.dp),
                ) {
                    Text(label, color = WoliText)
                }
            }
        }
    }
}
