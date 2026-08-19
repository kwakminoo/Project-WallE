package com.woli.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.woli.app.call.WoliCallActionController
import com.woli.app.call.WoliCallAccess
import com.woli.app.call.WoliCallCenter
import com.woli.app.call.WoliCallEvent
import com.woli.app.call.WoliCallMonitor
import com.woli.app.call.WoliCallMonitorStartResult
import com.woli.app.call.WoliCallState
import com.woli.app.call.WoliCallStateMapper
import com.woli.app.device.WoliBleDeviceClient
import com.woli.app.device.WoliDeviceCenter
import com.woli.app.device.WoliDeviceProtocol
import com.woli.app.device.WoliDeviceState
import com.woli.app.focus.WoliFocusExitReason
import com.woli.app.focus.WoliFocusGuardService
import com.woli.app.focus.WoliFocusSession
import com.woli.app.focus.WoliFocusSessionController
import com.woli.app.focus.formatDurationClock
import com.woli.app.focus.formatDurationKorean
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
import com.woli.app.voice.WoliVoiceCommandParser
import com.woli.app.voice.WoliVoiceCommandType
import kotlinx.coroutines.delay

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
    val deviceState by WoliDeviceCenter.state.collectAsState()
    val currentSession by WoliFocusSessionController.current.collectAsState()
    val savedConfig by WoliFocusSessionController.config.collectAsState()
    val focusConfig = currentSession?.config ?: savedConfig
    val accessEnabled = WoliNotificationAccess.isEnabled(context)
    val latestEvent = events.firstOrNull { event ->
        !focusConfig.allowImportantOnly || event.priority != WoliNotificationPriority.Normal
    }
    val audibleCall = currentCall?.takeIf { call ->
        !focusConfig.allowImportantOnly || call.isImportant
    }
    val ttsSpeaker = remember(context) { WoliTtsSpeaker(context) }
    val speechRecognizer = remember(context) { WoliSpeechRecognizer(context) }
    val bleClient = remember(context) { WoliBleDeviceClient(context) }
    val callMonitor = remember(context) {
        WoliCallMonitor(context.applicationContext) { state, callerInfo ->
            WoliCallCenter.updateState(state, callerInfo = callerInfo)
        }
    }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var autoCompleted by remember { mutableStateOf(false) }
    var handWarningHandled by remember { mutableStateOf(false) }
    var lastSpokenEventId by remember { mutableStateOf<String?>(null) }
    var lastSpokenCallKey by remember { mutableStateOf<String?>(null) }
    var replyDraft by remember { mutableStateOf<WoliReplyDraft?>(null) }
    var replyError by remember { mutableStateOf<String?>(null) }
    var replySendResult by remember { mutableStateOf<WoliReplySendResult?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isCommandListening by remember { mutableStateOf(false) }
    var pendingVoiceCommand by remember { mutableStateOf(false) }
    var commandMessage by remember { mutableStateOf<String?>(null) }
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

    LaunchedEffect(Unit) {
        WoliFocusSessionController.startIfNeeded(System.currentTimeMillis())
        while (true) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
            val activeSession = WoliFocusSessionController.current.value ?: continue
            if (!autoCompleted && activeSession.remainingMillis(nowMillis) <= 0L) {
                autoCompleted = true
                WoliFocusSessionController.complete(WoliFocusExitReason.Completed, nowMillis)
                WoliDeviceCenter.setLocked(false)
                bleClient.sendCommand(WoliDeviceProtocol.COMMAND_UNLOCK)
                WoliFocusGuardService.stop(context)
                onComplete()
            }
        }
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

    fun cancelReplyDraft() {
        speechRecognizer.cancel()
        replyDraft = null
        replyError = null
        replySendResult = null
        isListening = false
    }

    fun confirmReplyDraft(): WoliReplySendResult? {
        val draft = replyDraft
        if (draft == null) {
            replyError = "전송할 답장 초안이 없습니다."
            return null
        }

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
        return result
    }

    fun completeFocusFromCommand() {
        WoliFocusSessionController.complete(WoliFocusExitReason.Completed)
        WoliDeviceCenter.setLocked(false)
        bleClient.sendCommand(WoliDeviceProtocol.COMMAND_UNLOCK)
        WoliFocusGuardService.stop(context)
        onComplete()
    }

    fun handleVoiceCommand(spokenText: String) {
        val command = WoliVoiceCommandParser.parse(spokenText)
        when (command.type) {
            WoliVoiceCommandType.StartReply -> {
                val event = latestEvent
                if (event == null) {
                    commandMessage = "답장할 알림이 없습니다."
                } else {
                    commandMessage = "답장 내용을 말해주세요."
                    ttsSpeaker.speak("답장 내용을 말해주세요.")
                    beginVoiceReply(event)
                }
            }
            WoliVoiceCommandType.SendReply -> {
                val result = confirmReplyDraft()
                commandMessage = result?.userMessage() ?: "전송할 답장 초안이 없습니다."
                ttsSpeaker.speak(commandMessage ?: "")
            }
            WoliVoiceCommandType.CancelReply -> {
                cancelReplyDraft()
                commandMessage = "답장 초안을 취소했습니다."
                ttsSpeaker.speak("답장 초안을 취소했습니다.")
            }
            WoliVoiceCommandType.AnswerCall -> {
                val result = WoliCallActionController.answer(context)
                commandMessage = result.userMessage()
                ttsSpeaker.speak(result.userMessage())
            }
            WoliVoiceCommandType.DeclineCall -> {
                val result = WoliCallActionController.decline(context)
                commandMessage = result.userMessage()
                ttsSpeaker.speak(result.userMessage())
            }
            WoliVoiceCommandType.ReadRemainingTime -> {
                val remaining = currentSession?.remainingMillis(nowMillis) ?: 0L
                commandMessage = "남은 시간은 ${remaining.formatDurationKorean()}입니다."
                ttsSpeaker.speak(commandMessage ?: "")
            }
            WoliVoiceCommandType.ContinueFocus -> {
                commandMessage = "집중을 계속합니다."
                ttsSpeaker.speak("집중을 계속합니다.")
            }
            WoliVoiceCommandType.CompleteFocus -> {
                commandMessage = "집중을 완료합니다."
                ttsSpeaker.speak("집중을 완료합니다.")
                completeFocusFromCommand()
            }
            WoliVoiceCommandType.StartUnlockMission -> {
                commandMessage = "잠금 해제 미션으로 이동합니다."
                ttsSpeaker.speak("잠금 해제 미션으로 이동합니다.")
                onQuit()
            }
            WoliVoiceCommandType.Unknown -> {
                commandMessage = "명령을 이해하지 못했어요. 답장, 보내, 취소, 전화 받아, 거절, 남은 시간 중 하나로 말해주세요."
                ttsSpeaker.speak(commandMessage ?: "")
            }
        }
    }

    fun beginVoiceCommand() {
        if (isListening || isCommandListening) return
        if (!microphonePermissionGranted) {
            commandMessage = "마이크 권한이 있어야 음성 명령을 사용할 수 있습니다."
            return
        }

        ttsSpeaker.stop()
        commandMessage = "명령을 듣고 있습니다."
        isCommandListening = true
        speechRecognizer.startListening(
            onResult = { spokenText ->
                isCommandListening = false
                handleVoiceCommand(spokenText)
            },
            onError = { message ->
                isCommandListening = false
                commandMessage = message
            },
        )
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphonePermissionGranted = granted
        val event = pendingReplyEvent
        val shouldStartCommand = pendingVoiceCommand
        pendingReplyEvent = null
        pendingVoiceCommand = false

        if (granted && event != null) {
            beginVoiceReply(event)
        } else if (granted && shouldStartCommand) {
            beginVoiceCommand()
        } else if (!granted) {
            replyError = "마이크 권한이 있어야 음성 답장을 만들 수 있습니다."
            commandMessage = "마이크 권한이 없어 음성 명령을 사용할 수 없습니다."
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
        WoliFocusGuardService.setUiActive(true)
        onDispose {
            WoliFocusGuardService.setUiActive(false)
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

    LaunchedEffect(audibleCall?.id, audibleCall?.state) {
        val call = audibleCall ?: return@LaunchedEffect
        val announcement = WoliAnnouncementFormatter.callAnnouncement(call)
            ?: return@LaunchedEffect
        val callKey = "${call.id}_${call.state.name}"
        if (callKey == lastSpokenCallKey) return@LaunchedEffect

        ttsSpeaker.speak(announcement)
        lastSpokenCallKey = callKey
    }

    LaunchedEffect(deviceState.isHandNear) {
        if (deviceState.isHandNear && !handWarningHandled) {
            handWarningHandled = true
            WoliFocusSessionController.addHandWarning()
            onShowWarning()
        } else if (!deviceState.isHandNear) {
            handWarningHandled = false
        }
    }

    LaunchedEffect(accessEnabled, latestEvent?.id, audibleCall?.state) {
        if (latestEvent?.id != replyDraft?.eventId) {
            replyDraft = null
            replyError = null
            replySendResult = null
            isListening = false
        }
        if (!accessEnabled) return@LaunchedEffect

        val event = latestEvent ?: return@LaunchedEffect
        if (audibleCall?.state == WoliCallState.Ringing) return@LaunchedEffect
        if (event.id == lastSpokenEventId) return@LaunchedEffect

        val announcement = WoliAnnouncementFormatter.notificationAnnouncement(event)
            ?: return@LaunchedEffect
        ttsSpeaker.speak(announcement)
        lastSpokenEventId = event.id
    }

    LandscapeFocusScaffold {
        val visibleCall = audibleCall?.takeIf { it.id != dismissedCallId }
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
        FocusSessionStatusPanel(
            session = currentSession,
            nowMillis = nowMillis,
            deviceState = deviceState,
        )
        Spacer(modifier = Modifier.height(10.dp))
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
                    dismissedCallId = audibleCall?.id
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
                confirmReplyDraft()
            },
            onCancelDraft = {
                cancelReplyDraft()
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        FocusVoiceCommandPanel(
            isListening = isCommandListening,
            message = commandMessage,
            onStart = {
                if (microphonePermissionGranted) {
                    beginVoiceCommand()
                } else {
                    pendingVoiceCommand = true
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        )
        Spacer(modifier = Modifier.height(14.dp))
        DemoChipRow(
            chips = listOf(
                "남은시간" to onShowRemaining,
                "중요연락" to onShowCall,
                "손접근" to {
                    WoliFocusSessionController.addHandWarning()
                    WoliDeviceCenter.setHandNear(true)
                    onShowWarning()
                },
                "완료" to {
                    completeFocusFromCommand()
                },
                "명령" to {
                    if (microphonePermissionGranted) {
                        beginVoiceCommand()
                    } else {
                        pendingVoiceCommand = true
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                "해제" to onQuit,
            ),
        )
    }
}

@Composable
private fun FocusSessionStatusPanel(
    session: WoliFocusSession?,
    nowMillis: Long,
    deviceState: WoliDeviceState,
) {
    val remaining = session?.remainingMillis(nowMillis) ?: 0L
    Row(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .background(Color(0xFF141414), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniStat("남은 시간", remaining.formatDurationClock())
        MiniStat("잠금", if (deviceState.isLocked) "작동" else "해제")
        MiniStat("손 접근", "${session?.handWarningCount ?: 0}회")
    }
}

@Composable
private fun FocusVoiceCommandPanel(
    isListening: Boolean,
    message: String?,
    onStart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .background(Color(0xFF101820), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isListening) "음성 명령 듣는 중" else "음성 명령",
                color = if (isListening) WoliYellow else WoliCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message ?: "답장, 보내, 취소, 전화 받아, 거절, 남은 시간, 해제 미션",
                color = WoliMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DemoChip(if (isListening) "듣는중" else "말하기", onStart)
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
            text = "판단 기준 · ${latestEvent.importanceReason.label}",
            color = WoliMuted,
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
                Text(
                    text = if (callEvent.isImportant) "중요 연락처" else "일반 전화",
                    color = if (callEvent.isImportant) WoliYellow else WoliMuted,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (callEvent.state == WoliCallState.Ringing) {
                "월이가 음성으로 안내했습니다. 전화 제어 권한이 있으면 받기/거절을 시도할 수 있습니다."
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
    val currentSession by WoliFocusSessionController.current.collectAsState()
    val savedConfig by WoliFocusSessionController.config.collectAsState()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val remaining = currentSession?.remainingMillis(nowMillis) ?: 0L
    val config = currentSession?.config ?: savedConfig

    LandscapeFocusScaffold {
        Text(
            text = "남은 시간  ${remaining.formatDurationClock()}",
            color = WoliText,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(28.dp))
        WoliEyes(mood = EyeMood.Idle, eyeSize = 80.dp, gap = 64.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "목표 ${config.durationMinutes}분 · 중요 알림만 ${if (config.allowImportantOnly) "허용" else "전체 확인"}",
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
    val context = LocalContext.current
    val callEvent by WoliCallCenter.current.collectAsState()
    val title = callEvent?.callerLabel ?: WoliCallEvent.UNKNOWN_CALLER_LABEL
    val status = callEvent?.state?.let(WoliCallStateMapper::displayLabel) ?: "전화 감지 대기"
    val announcement = callEvent?.let { event ->
        WoliAnnouncementFormatter.callAnnouncement(event)
            ?: "통화 중에는 통화 음성을 방해하지 않도록 추가 안내하지 않습니다."
    } ?: "전화가 오면 월이가 음성으로 안내합니다."
    var callControlGranted by remember { mutableStateOf(WoliCallActionController.canControlCalls(context)) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val callControlPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        callControlGranted = granted
        actionMessage = if (granted) {
            "전화 제어 권한이 허용되었습니다. 다시 선택하세요."
        } else {
            "전화 제어 권한이 없어 휴대폰 통화 화면에서 직접 처리해야 합니다."
        }
    }

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
        if (actionMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = actionMessage ?: "",
                color = if (callControlGranted) WoliCyan else WoliWarning,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DemoChip("받기") {
                if (!callControlGranted) {
                    callControlPermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS)
                } else {
                    actionMessage = WoliCallActionController.answer(context).userMessage()
                    onAnswer()
                }
            }
            DemoChip("거절") {
                if (!callControlGranted) {
                    callControlPermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS)
                } else {
                    actionMessage = WoliCallActionController.decline(context).userMessage()
                    onLater()
                }
            }
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
        DemoChip("손이 멀어짐") {
            WoliDeviceCenter.setHandNear(false)
            onDismiss()
        }
    }
}

@Composable
fun FocusCompleteScreen(onReport: () -> Unit, onHome: () -> Unit) {
    BackHandler(onBack = onHome)
    val callHistory by WoliCallCenter.history.collectAsState()
    val completedSession = WoliFocusSessionController.latestCompleted()
    val elapsed = completedSession?.elapsedMillis(completedSession.completedAtMillis ?: System.currentTimeMillis()) ?: 0L

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
            MiniStat("집중 시간", elapsed.formatDurationKorean())
            MiniStat("경고", "${completedSession?.handWarningCount ?: 0}회")
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
    val context = LocalContext.current
    val pattern = remember { listOf(0, 1, 2, 1, 0, 2, 0, 1, 2, 2, 1, 0) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(10) }
    var finished by remember { mutableStateOf(false) }
    val targetLane = pattern[index % pattern.size]

    fun submitLane(lane: Int) {
        if (finished) return
        if (lane == targetLane) {
            score += 1
        } else {
            misses += 1
        }
        index += 1
        if (score >= 8) {
            finished = true
            WoliFocusSessionController.recordMissionAttempt(success = true)
            WoliFocusSessionController.complete(WoliFocusExitReason.MissionUnlocked)
            WoliDeviceCenter.setLocked(false)
            WoliFocusGuardService.stop(context)
            onSuccess()
        }
    }

    LaunchedEffect(finished) {
        while (!finished && remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds -= 1
        }
        if (!finished && remainingSeconds <= 0) {
            finished = true
            WoliFocusSessionController.recordMissionAttempt(success = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SCORE $score / 8", color = WoliYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("TIME ${remainingSeconds}s · MISS $misses", color = WoliCyan, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("리듬 미션 · 빛나는 레인을 순서대로 탭하세요", color = WoliMuted, fontSize = 13.sp)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            RhythmShellVisual(targetLane = targetLane)
        }
        Text(
            text = if (finished && score < 8) "실패: 다시 시도하거나 집중을 계속하세요" else "현재 목표 레인 ${targetLane + 1}",
            color = if (finished && score < 8) WoliWarning else WoliText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(0.72f)) {
            repeat(3) { lane ->
                Box(modifier = Modifier.weight(1f)) {
                    WoliSecondaryButton(
                        text = "${lane + 1}",
                        onClick = { submitLane(lane) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(0.72f)) {
            Box(modifier = Modifier.weight(1f)) { WoliSecondaryButton("취소", onCancel) }
            Box(modifier = Modifier.weight(1f)) {
                WoliPrimaryButton(
                    text = "다시 시도",
                    onClick = {
                        index = 0
                        score = 0
                        misses = 0
                        remainingSeconds = 10
                        finished = false
                    },
                )
            }
        }
    }
}

@Composable
fun SessionReportScreen(onHome: () -> Unit) {
    BackHandler(onBack = onHome)
    val context = LocalContext.current
    LaunchedEffect(context) {
        WoliReplyHistoryStore.load(context)
    }
    val replyHistory by WoliReplyHistoryStore.entries.collectAsState()
    val callHistory by WoliCallCenter.history.collectAsState()
    val currentSession by WoliFocusSessionController.current.collectAsState()
    val completedSession = WoliFocusSessionController.latestCompleted()
    val reportSession = completedSession ?: currentSession
    val reportNow = completedSession?.completedAtMillis ?: System.currentTimeMillis()
    val elapsed = reportSession?.elapsedMillis(reportNow) ?: 0L
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
            ReportCard("총 집중", elapsed.formatDurationKorean())
            ReportCard("미션", "${reportSession?.missionSuccessCount ?: 0}/${reportSession?.missionAttemptCount ?: 0}")
            ReportCard("손 접근", "${reportSession?.handWarningCount ?: 0}회")
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
private fun RhythmShellVisual(targetLane: Int) {
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
                val active = i == targetLane
                drawLine(laneColors[i], origin, t, strokeWidth = 4f)
                drawCircle(laneColors[i].copy(alpha = 0.35f), radius = 28f, center = Offset(
                    origin.x + (t.x - origin.x) * 0.45f,
                    origin.y + (t.y - origin.y) * 0.45f,
                ))
                drawCircle(Color.White.copy(alpha = if (active) 0.48f else 0.2f), radius = if (active) 42f else 34f, center = t)
                drawCircle(laneColors[i], radius = if (active) 24f else 18f, center = t)
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
