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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.notification.WoliNotificationAllowedAppTarget
import com.woli.app.notification.WoliNotificationMode
import com.woli.app.notification.WoliNotificationRuleCatalog
import com.woli.app.notification.WoliNotificationRuleConfig
import com.woli.app.notification.WoliNotificationRuleStore
import com.woli.app.ui.components.ShellHintBar
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliWarning
import com.woli.app.ui.theme.WoliYellow

@Composable
fun NotificationPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val config by WoliNotificationRuleStore.config.collectAsState()

    LaunchedEffect(context) {
        WoliNotificationRuleStore.load(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "집중 알림 기준", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "일반 메신저는 조용히 넘기고, 중요한 사람과 상황만 월이가 안내합니다.",
            color = WoliMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            PolicyModeSection(
                selectedMode = config.mode,
                onSelect = { mode -> WoliNotificationRuleStore.updateMode(context, mode) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            AllowedAppsSection(
                config = config,
                onToggle = { target, enabled ->
                    WoliNotificationRuleStore.setAllowedTarget(
                        context = context,
                        targetId = target.id,
                        enabled = enabled,
                    )
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            PolicyRulesCard(config = config)
            Spacer(modifier = Modifier.height(12.dp))
            ShellHintBar(
                text = "중요 연락처는 설정 > 중요 연락처에서 관리합니다. 광고, 묶음, 재생 중 알림은 모드와 관계없이 음성 안내에서 제외됩니다.",
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        WoliSecondaryButton(
            text = "기본값으로 복원",
            onClick = { WoliNotificationRuleStore.resetToDefault(context) },
        )
    }
}

@Composable
private fun PolicyModeSection(
    selectedMode: WoliNotificationMode,
    onSelect: (WoliNotificationMode) -> Unit,
) {
    val modes = WoliNotificationMode.values()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("정책 모드", color = WoliText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        modes.forEach { mode ->
            ModeOption(
                mode = mode,
                selected = mode == selectedMode,
                onClick = { onSelect(mode) },
            )
            if (mode != modes.last()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ModeOption(
    mode: WoliNotificationMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) WoliYellow else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) WoliYellow.copy(alpha = 0.13f) else Color(0xFF141414),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(borderColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (selected) "선택됨" else "선택",
                    color = if (selected) WoliBlack else WoliMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mode.label,
                color = WoliText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = mode.summary,
            color = WoliMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun AllowedAppsSection(
    config: WoliNotificationRuleConfig,
    onToggle: (WoliNotificationAllowedAppTarget, Boolean) -> Unit,
) {
    val appliesAllowedApps = config.allowsUserAllowedApps()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("허용 앱 예외", color = WoliText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = if (appliesAllowedApps) {
                        "켜진 앱의 1:1 메시지만 중요 알림 후보가 됩니다."
                    } else {
                        "집중 우선 모드에서는 앱 예외를 적용하지 않습니다."
                    },
                    color = WoliMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            Text(
                text = if (appliesAllowedApps) "적용" else "비활성",
                color = if (appliesAllowedApps) WoliCyan else WoliMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        WoliNotificationRuleCatalog.allowedAppTargets.forEach { target ->
            AllowedAppRow(
                target = target,
                checked = target.packageNames.any { it in config.allowedPackageNames },
                enabled = appliesAllowedApps,
                onCheckedChange = { checked -> onToggle(target, checked) },
            )
            if (target != WoliNotificationRuleCatalog.allowedAppTargets.last()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AllowedAppRow(
    target: WoliNotificationAllowedAppTarget,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = target.displayName,
                color = if (enabled) WoliText else WoliMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = target.packageNames.joinToString(),
                color = WoliMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WoliBlack,
                checkedTrackColor = WoliYellow,
                uncheckedThumbColor = WoliMuted,
                uncheckedTrackColor = Color(0xFF2A2A2D),
            ),
        )
    }
}

@Composable
private fun PolicyRulesCard(config: WoliNotificationRuleConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("판단 순서", color = WoliText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        PolicyRuleRow("전화", "긴급", WoliWarning)
        PolicyRuleRow("중요 연락처", "중요", WoliYellow)
        PolicyRuleRow("중요 연락처 + 긴급 표현", "긴급", WoliWarning)
        PolicyRuleRow("알람 / 일정 / 리마인더", "중요", WoliYellow)
        PolicyRuleRow("일반 카톡 / 문자 / DM", "제외", WoliMuted)
        if (config.mode == WoliNotificationMode.Demo) {
            PolicyRuleRow("답장 가능한 테스트 메시지", "중요", WoliCyan)
        }
        PolicyRuleRow("광고 / 쿠폰 / 좋아요 / 묶음 / 재생 중", "제외", WoliMuted)
    }
}

@Composable
private fun PolicyRuleRow(label: String, result: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = WoliMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = result,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
