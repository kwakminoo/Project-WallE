package com.woli.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.notification.WoliNotificationAccess
import com.woli.app.notification.WoliNotificationCenter
import com.woli.app.notification.WoliReplyAppValidationCatalog
import com.woli.app.notification.WoliReplyAppValidationStage
import com.woli.app.notification.WoliReplyAppValidationStatus
import com.woli.app.notification.WoliReplyHistoryStore
import com.woli.app.ui.components.ShellHintBar
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliWarning
import com.woli.app.ui.theme.WoliYellow

@Composable
fun NotificationDiagnosticsScreen(
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val context = LocalContext.current
    val events by WoliNotificationCenter.events.collectAsState()
    val replyHistory by WoliReplyHistoryStore.entries.collectAsState()
    var refreshKey by remember { mutableIntStateOf(0) }
    var accessEnabled by remember { mutableStateOf(WoliNotificationAccess.isEnabled(context)) }

    LaunchedEffect(context) {
        WoliReplyHistoryStore.load(context)
    }

    val installedPackages = remember(context, refreshKey) {
        WoliReplyAppValidationCatalog.installedKnownPackages(context)
    }
    val statuses = remember(installedPackages, events, replyHistory) {
        WoliReplyAppValidationCatalog.buildStatuses(
            installedPackages = installedPackages,
            events = events,
            history = replyHistory,
        )
    }
    val readyCount = statuses.count { it.stage == WoliReplyAppValidationStage.Ready }
    val attentionCount = statuses.count { it.needsAttention }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "앱별 알림 검증", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "실제 알림 수신, 답장 액션 감지, 전송 결과를 앱별로 확인합니다.",
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
            ValidationSummaryCard(
                accessEnabled = accessEnabled,
                readyCount = readyCount,
                attentionCount = attentionCount,
                observedEventCount = events.size,
                replyHistoryCount = replyHistory.size,
            )
            Spacer(modifier = Modifier.height(12.dp))
            statuses.forEach { status ->
                ValidationAppCard(status = status)
                Spacer(modifier = Modifier.height(10.dp))
            }
            ShellHintBar(
                text = "검증 완료는 해당 앱에서 답장 전송 성공 이력이 1회 이상 남았을 때 표시됩니다.",
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        WoliSecondaryButton(
            text = "알림 접근 설정 열기",
            onClick = onOpenNotificationSettings,
        )
        Spacer(modifier = Modifier.height(10.dp))
        WoliPrimaryButton(
            text = "상태 새로고침",
            onClick = {
                refreshKey += 1
                accessEnabled = WoliNotificationAccess.isEnabled(context)
            },
        )
    }
}

@Composable
private fun ValidationSummaryCard(
    accessEnabled: Boolean,
    readyCount: Int,
    attentionCount: Int,
    observedEventCount: Int,
    replyHistoryCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("검증 요약", color = WoliText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryMetric(
                label = "알림 접근",
                value = if (accessEnabled) "허용" else "필요",
                active = accessEnabled,
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = "완료",
                value = "${readyCount}개",
                active = readyCount > 0,
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = "확인 필요",
                value = "${attentionCount}개",
                active = attentionCount == 0,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "관측 알림 ${observedEventCount}개 · 답장 기록 ${replyHistoryCount}개",
            color = WoliMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF141414), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = WoliMuted, fontSize = 11.sp, maxLines = 1)
        Text(
            text = value,
            color = if (active) WoliCyan else WoliWarning,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ValidationAppCard(status: WoliReplyAppValidationStatus) {
    val statusColor = when {
        status.failedCount > 0 -> WoliWarning
        status.stage == WoliReplyAppValidationStage.Ready -> WoliCyan
        status.stage == WoliReplyAppValidationStage.ReplyActionObserved -> WoliYellow
        status.stage == WoliReplyAppValidationStage.NotificationObserved -> WoliYellow
        else -> WoliMuted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.target.displayName,
                    color = WoliText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = status.installedPackageName ?: "대상 패키지 미감지",
                    color = WoliMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(
                label = WoliReplyAppValidationCatalog.stageLabel(status.stage),
                color = statusColor,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactMetric("알림", "${status.notificationCount}", Modifier.weight(1f))
            CompactMetric("답장 액션", "${status.replyActionCount}", Modifier.weight(1f))
            CompactMetric("성공", "${status.sentCount}", Modifier.weight(1f))
            CompactMetric("실패", "${status.failedCount}", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = status.latestEventTitle ?: status.target.testPrompt,
            color = WoliMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (status.latestResultMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status.latestResultMessage,
                color = if (status.failedCount > 0) WoliWarning else WoliCyan,
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF141414), RoundedCornerShape(10.dp))
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = WoliMuted, fontSize = 10.sp, maxLines = 1)
        Text(value, color = WoliText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
