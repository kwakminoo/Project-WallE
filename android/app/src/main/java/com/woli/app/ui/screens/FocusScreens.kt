package com.woli.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.woli.app.call.WoliCallAccess
import com.woli.app.call.WoliCallCenter
import com.woli.app.call.WoliCallEvent
import com.woli.app.call.WoliCallMonitor
import com.woli.app.call.WoliCallMonitorStartResult
import com.woli.app.call.WoliCallState
import com.woli.app.call.WoliCallStateMapper
import com.woli.app.notification.WoliNotificationAccess
import com.woli.app.notification.WoliNotificationCenter
import com.woli.app.notification.WoliNotificationEvent
import com.woli.app.notification.WoliNotificationPriority
import com.woli.app.notification.WoliRemoteReplyActionStore
import com.woli.app.notification.WoliReplyHistoryEntry
import com.woli.app.notification.WoliReplyHistoryResultType
import com.woli.app.notification.WoliReplyHistoryStore
import com.woli.app.notification.WoliReplySendResult
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
import com.woli.app.voice.WoliAnnouncementFormatter
import com.woli.app.voice.WoliReplyDraft
import com.woli.app.voice.WoliSpeechRecognizer
import com.woli.app.voice.WoliTtsSpeaker

@Composable
fun FocusEyesScreen(
    onShowRemaining: () -> Unit,
    onShowCall: () -> Unit,
    onShowWarning: () -> Unit,
    onQuit: () -> Unit,
    onComplete: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val context = LocalContext.current
    val events by WoliNotificationCenter.events.collectAsState()
    val currentCall by WoliCallCenter.current.collectAsState()
    val accessEnabled = WoliNotificationAccess.isEnabled(context)
    val latestEvent = events.firstOrNull()
    val ttsSpeaker = remember(context) { WoliTtsSpeaker(context) }
    val speechRecognizer = remember(context) { WoliSpeechRecognizer(context) }
    val callMonitor = remember(context) {
        WoliCallMonitor(context.applicationContext) { state ->
            WoliCallCenter.updateState(state)
        }
    }
    var lastSpokenEventId by remember { mutableStateOf<String?>(null) }
    var lastSpokenCallKey by remember { mutableStateOf<String?>(null) }
    var replyDraft by remember { mutableStateOf<WoliReplyDraft?>(null) }
    var replyError by remember { mutableStateOf<String?>(null) }
    var replySendResult by remember { mutableStateOf<WoliReplySendResult?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var pendingReplyEvent by remember { mutableStateOf<WoliNotificationEvent?>(null) }
    var dismissedCallId by remember { mutableStateOf<String?>(null) }
    var callMonitorResult by remember {
        mutableStateOf<WoliCallMonitorStartResult>(WoliCallMonitorStartResult.MissingPermission)
    }
    var microphonePermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var phonePermissionGranted by remember {
        mutableStateOf(WoliCallAccess.isGranted(context))
    }

    LaunchedEffect(context) {
        WoliReplyHistoryStore.load(context)
    }

    fun beginVoiceReply(event: WoliNotificationEvent) {
        if (!event.canReply) {
            replyError = "이 알림은 답장을 지원하지 않습니다."
            return
        }

        ttsSpeaker.stop()
        replyDraft = null
        replyError = null
        replySendResult = null
        isListening = true
        speechRecognizer.startListening(
            onResult = { spokenText ->
                replyDraft = WoliReplyDraft(eventId = event.id, replyText = spokenText)
                replyError = null
                isListening = false
            },
            onError = { message ->
                replyError = message
                isListening = false
            },
        )
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphonePermissionGranted = granted
        val event = pendingReplyEvent
        pendingReplyEvent = null

        if (granted && event != null) {
            beginVoiceReply(event)
        } else if (!granted) {
            replyError = "마이크 권한이 있어야 음성 답장을 만들 수 있습니다."
        }
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        phonePermissionGranted = granted
        if (!granted) {
            callMonitorResult = WoliCallMonitorStartResult.MissingPermission
        }
    }

    DisposableEffect(ttsSpeaker, speechRecognizer) {
        onDispose {
            ttsSpeaker.shutdown()
            speechRecognizer.shutdown()
        }
    }

    DisposableEffect(callMonitor, phonePermissionGranted) {
        if (phonePermissionGranted) {
            callMonitorResult = callMonitor.start()
        } else {
            callMonitorResult = WoliCallMonitorStartResult.MissingPermission
        }

        onDispose {
            callMonitor.stop()
        }
    }

    LaunchedEffect(currentCall?.id, currentCall?.state) {
        val call = currentCall ?: return@LaunchedEffect
        val announcement = WoliAnnouncementFormatter.callAnnouncement(call)
            ?: return@LaunchedEffect
        val callKey = "${call.id}_${call.state.name}"
        if (callKey == lastSpokenCallKey) return@LaunchedEffect

        ttsSpeaker.speak(announcement)
        lastSpokenCallKey = callKey
    }

    LaunchedEffect(accessEnabled, latestEvent?.id, currentCall?.state) {
        if (latestEvent?.id != replyDraft?.eventId) {
            replyDraft = null
            replyError = null
            replySendResult = null
            isListening = false
        }
        if (!accessEnabled) return@LaunchedEffect

        val event = latestEvent ?: return@LaunchedEffect
        if (currentCall?.state == WoliCallState.Ringing) return@LaunchedEffect
        if (event.id == lastSpokenEventId) return@LaunchedEffect

        val announcement = WoliAnnouncementFormatter.notificationAnnouncement(event)
            ?: return@LaunchedEffect
        ttsSpeaker.speak(announcement)
        lastSpokenEventId = event.id
    }

    LandscapeFocusScaffold {
        val visibleCall = currentCall?.takeIf { it.id != dismissedCallId }
        WoliEyes(
            mood = when {
                visibleCall?.state == WoliCallState.Ringing -> EyeMood.Happy
                latestEvent != null -> EyeMood.Alert
                else -> EyeMood.Idle
            },
            eyeSize = 88.dp,
            gap = 72.dp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (!phonePermissionGranted || visibleCall != null || !callMonitorResult.isStarted) {
            FocusCallPanel(
                phonePermissionGranted = phonePermissionGranted,
                callEvent = visibleCall,
                monitorResult = callMonitorResult,
                onRequestPermission = {
                    phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                },
                onOpenCallScreen = onShowCall,
                onDismiss = {
                    dismissedCallId = currentCall?.id
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        FocusNotificationPanel(
            accessEnabled = accessEnabled,
            latestEvent = latestEvent,
            replyDraft = replyDraft,
            replyError = replyError,
            replySendResult = replySendResult,
            isListening = isListening,
            onOpenSettings = onOpenNotificationSettings,
            onStartVoiceReply = { event ->
                if (microphonePermissionGranted) {
                    beginVoiceReply(event)
                } else {
                    pendingReplyEvent = event
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onConfirmDraft = {
                val draft = replyDraft
                if (draft == null) {
                    replyError = "전송할 답장 초안이 없습니다."
                } else {
                    val draftEvent = events.firstOrNull { it.id == draft.eventId }
                    val result = WoliRemoteReplyActionStore.sendReply(
                        context = context,
                        eventId = draft.eventId,
                        replyText = draft.replyText,
                    )
                    replySendResult = result
                    if (draftEvent != null) {
                        WoliReplyHistoryStore.record(
                            context = context,
                            event = draftEvent,
                            replyText = draft.replyText,
                            result = result,
                        )
                    }
                    if (result.isSuccess) {
                        replyDraft = draft.copy(confirmed = true)
                        replyError = null
                    } else {
                        replyError = result.userMessage()
                    }
                }
            },
            onCancelDraft = {
                speechRecognizer.cancel()
                replyDraft = null
                replyError = null
                replySendResult = null
                isListening = false
            },
        )
        Spacer(modifier = Modifier.height(14.dp))
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
private fun FocusNotificationPanel(
    accessEnabled: Boolean,
    latestEvent: WoliNotificationEvent?,
    replyDraft: WoliReplyDraft?,
    replyError: String?,
    replySendResult: WoliReplySendResult?,
    isListening: Boolean,
    onOpenSettings: () -> Unit,
    onStartVoiceReply: (WoliNotificationEvent) -> Unit,
    onConfirmDraft: () -> Unit,
    onCancelDraft: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .background(Color(0xFF141414), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!accessEnabled) {
            Text(
                text = "알림 접근 권한 필요",
                color = WoliYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "중요 알림을 감지하려면 설정에서 월이를 허용하세요.",
                color = WoliMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            DemoChip("설정 열기", onOpenSettings)
            return@Column
        }

        if (latestEvent == null) {
            Text(
                text = "중요 알림 수신 대기 중",
                color = WoliCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "집중 중 들어오는 알림을 월이가 분류합니다.",
                color = WoliMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        Text(
            text = "${priorityLabel(latestEvent.priority)} · ${latestEvent.appName}",
            color = priorityColor(latestEvent.priority),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = latestEvent.title,
            color = WoliText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = latestEvent.preview,
            color = WoliMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (latestEvent.canReply) "답장 가능 알림" else "읽기 전용 알림",
            color = if (latestEvent.canReply) WoliCyan else WoliMuted,
            fontSize = 12.sp,
        )
        Text(
            text = if (latestEvent.priority == WoliNotificationPriority.Normal) {
                "음성 안내 제외"
            } else {
                "월이가 음성으로 안내합니다"
            },
            color = if (latestEvent.priority == WoliNotificationPriority.Normal) WoliMuted else WoliYellow,
            fontSize = 12.sp,
        )
        if (latestEvent.canReply) {
            Spacer(modifier = Modifier.height(8.dp))
            VoiceReplySection(
                event = latestEvent,
                replyDraft = replyDraft?.takeIf { it.eventId == latestEvent.id },
                replyError = replyError,
                replySendResult = replySendResult,
                isListening = isListening,
                onStartVoiceReply = onStartVoiceReply,
                onConfirmDraft = onConfirmDraft,
                onCancelDraft = onCancelDraft,
            )
        }
    }
}

@Composable
private fun FocusCallPanel(
    phonePermissionGranted: Boolean,
    callEvent: WoliCallEvent?,
    monitorResult: WoliCallMonitorStartResult,
    onRequestPermission: () -> Unit,
    onOpenCallScreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .background(Color(0xFF141414), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!phonePermissionGranted) {
            Text(
                text = "전화 감지 권한 필요",
                color = WoliYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "집중 중 걸려오는 전화를 음성으로 안내하려면 전화 상태 권한이 필요합니다.",
                color = WoliMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            DemoChip("권한 허용", onRequestPermission)
            return@Column
        }

        if (callEvent == null) {
            Text(
                text = monitorResult.userMessage(),
                color = if (monitorResult.isStarted) WoliCyan else WoliWarning,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = WoliYellow,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = WoliCallStateMapper.displayLabel(callEvent.state),
                    color = if (callEvent.state == WoliCallState.Ringing) WoliYellow else WoliCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = callEvent.callerLabel,
                    color = WoliText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (callEvent.state == WoliCallState.Ringing) {
                "월이가 음성으로 안내했습니다. 실제 수락/거절은 휴대폰 통화 화면에서 진행하세요."
            } else {
                "통화 상태를 감지하고 집중 기록에 남깁니다."
            },
            color = WoliMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoChip("전화 화면", onOpenCallScreen)
            DemoChip("집중 계속", onDismiss)
        }
    }
}

@Composable
private fun VoiceReplySection(
    event: WoliNotificationEvent,
    replyDraft: WoliReplyDraft?,
    replyError: String?,
    replySendResult: WoliReplySendResult?,
    isListening: Boolean,
    onStartVoiceReply: (WoliNotificationEvent) -> Unit,
    onConfirmDraft: () -> Unit,
    onCancelDraft: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            replyDraft?.confirmed == true -> {
                Text(
                    text = replySendResult?.userMessage() ?: "답장 전송 요청을 완료했어요.",
                    color = WoliCyan,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "“${replyDraft.replyText}”",
                    color = WoliText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            replyDraft != null -> {
                if (replySendResult != null && !replySendResult.isSuccess) {
                    Text(
                        text = replySendResult.userMessage(),
                        color = WoliWarning,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = "이렇게 답장할까요?",
                    color = WoliYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "“${replyDraft.replyText}”",
                    color = WoliText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DemoChip(
                        label = if (replySendResult != null && !replySendResult.isSuccess) {
                            "재전송"
                        } else {
                            "전송"
                        },
                        onClick = onConfirmDraft,
                    )
                    DemoChip("다시 말하기") { onStartVoiceReply(event) }
                    DemoChip("취소", onCancelDraft)
                }
            }
            else -> {
                if (replyError != null) {
                    Text(
                        text = replyError,
                        color = WoliWarning,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                DemoChip(
                    label = if (isListening) "듣는 중…" else "음성 답장 말하기",
                    onClick = { if (!isListening) onStartVoiceReply(event) },
                )
            }
        }
    }
}

private fun priorityLabel(priority: WoliNotificationPriority): String {
    return when (priority) {
        WoliNotificationPriority.Critical -> "긴급"
        WoliNotificationPriority.Important -> "중요"
        WoliNotificationPriority.Normal -> "일반"
    }
}

private fun priorityColor(priority: WoliNotificationPriority): Color {
    return when (priority) {
        WoliNotificationPriority.Critical -> WoliWarning
        WoliNotificationPriority.Important -> WoliYellow
        WoliNotificationPriority.Normal -> WoliMuted
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
    val callEvent by WoliCallCenter.current.collectAsState()
    val title = callEvent?.callerLabel ?: WoliCallEvent.UNKNOWN_CALLER_LABEL
    val status = callEvent?.state?.let(WoliCallStateMapper::displayLabel) ?: "전화 감지 대기"
    val announcement = callEvent?.let { event ->
        WoliAnnouncementFormatter.callAnnouncement(event)
            ?: "통화 중에는 통화 음성을 방해하지 않도록 추가 안내하지 않습니다."
    } ?: "전화가 오면 월이가 음성으로 안내합니다."

    LandscapeFocusScaffold {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WoliEyes(mood = EyeMood.Happy, eyeSize = 70.dp, gap = 48.dp)
            Spacer(modifier = Modifier.width(20.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Call, contentDescription = null, tint = WoliYellow, modifier = Modifier.size(28.dp))
                Text(title, color = WoliText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(status, color = WoliMuted, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "TTS: \"$announcement\"",
            color = WoliCyan,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DemoChip("휴대폰에서 받기", onAnswer)
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
    val callHistory by WoliCallCenter.history.collectAsState()

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
            MiniStat("전화 감지", "${callHistory.size}회")
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
    val context = LocalContext.current
    LaunchedEffect(context) {
        WoliReplyHistoryStore.load(context)
    }
    val replyHistory by WoliReplyHistoryStore.entries.collectAsState()
    val callHistory by WoliCallCenter.history.collectAsState()
    val sentReplyCount = replyHistory.count { it.resultType == WoliReplyHistoryResultType.Sent }
    val failedReplyCount = replyHistory.count { it.resultType != WoliReplyHistoryResultType.Sent }

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
            ReportCard("답장 성공", "${sentReplyCount}회")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReportCard("연속 집중", "6일")
            ReportCard("친밀도", "+12 XP")
            ReportCard("답장 실패", "${failedReplyCount}회")
            ReportCard("전화 감지", "${callHistory.size}회")
        }
        Spacer(modifier = Modifier.height(14.dp))
        ReplyHistorySummaryPanel(replyHistory.firstOrNull())
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.width(280.dp)) {
            WoliPrimaryButton(text = "홈으로 돌아가기", onClick = onHome)
        }
    }
}

@Composable
private fun ReplyHistorySummaryPanel(latestEntry: WoliReplyHistoryEntry?) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "최근 답장 기록",
            color = WoliMuted,
            fontSize = 12.sp,
        )
        if (latestEntry == null) {
            Text(
                text = "아직 전송 기록이 없습니다.",
                color = WoliText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        val statusText = if (latestEntry.resultType == WoliReplyHistoryResultType.Sent) {
            "성공"
        } else {
            "확인 필요"
        }
        val statusColor = if (latestEntry.resultType == WoliReplyHistoryResultType.Sent) {
            WoliCyan
        } else {
            WoliWarning
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$statusText · ${latestEntry.appName}",
            color = statusColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = latestEntry.title,
            color = WoliText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "답장: ${latestEntry.replyText}",
            color = WoliMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (latestEntry.resultType != WoliReplyHistoryResultType.Sent) {
            Text(
                text = latestEntry.resultMessage,
                color = WoliWarning,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
