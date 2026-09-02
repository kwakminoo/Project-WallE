package com.woli.app.focus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import kotlinx.coroutines.awaitCancellation
import com.woli.app.MainActivity
import com.woli.app.call.WoliCallActionController
import com.woli.app.call.WoliCallCenter
import com.woli.app.call.WoliCallState
import com.woli.app.device.WoliBleDeviceClient
import com.woli.app.focus.hand.CameraHandApproachAccess
import com.woli.app.focus.hand.WoliCameraHandApproachCenter
import com.woli.app.navigation.FocusSessionNav
import com.woli.app.navigation.Routes
import com.woli.app.notification.WoliNotificationCenter
import com.woli.app.notification.WoliNotificationEvent
import com.woli.app.notification.WoliNotificationPriority
import com.woli.app.notification.WoliRemoteReplyActionStore
import com.woli.app.notification.WoliReplyHistoryStore
import com.woli.app.notification.WoliReplySendResult
import com.woli.app.voice.WoliReplyDraft
import com.woli.app.voice.WoliSpeechRecognizer
import com.woli.app.voice.WoliTtsSpeaker
import com.woli.app.voice.WoliVoiceCommandParser
import com.woli.app.voice.WoliVoiceCommandType
import kotlinx.coroutines.delay

enum class HandWarningCause {
    HandNear,
    EscapeAttempt,
}

/** 손접근/이탈 시도로 경고 화면에 진입한 이유. Compose 메인 스레드 전용. */
object FocusHandWarningState {
    var entryCause: HandWarningCause? = null
}

/** 손 접근 내비게이션: 세션 시작 직후 오탐·이미 손이 가까운 상태를 무시한다. */
const val HAND_APPROACH_NAV_GRACE_MS = 4_000L

fun handWarningReturnDelayMs(cause: HandWarningCause?): Long = when (cause) {
    HandWarningCause.HandNear -> 2_000L
    HandWarningCause.EscapeAttempt -> 900L
    null -> 2_000L
}

/** 홈/탭/뒤로가기 이탈 시도 시 손접근 경고 화면으로 전환한다. */
@Composable
fun FocusEscapeNavigation(
    navController: NavController,
    currentRoute: String?,
    currentFocusSession: WoliFocusSession?,
) {
    val attemptNonce by FocusEscapeAttemptCenter.nonce.collectAsState()
    val isHandNear by WoliCameraHandApproachCenter.isHandNear.collectAsState()

    LaunchedEffect(attemptNonce, currentRoute, currentFocusSession?.id, isHandNear) {
        if (currentFocusSession == null || attemptNonce == 0L) return@LaunchedEffect
        if (currentRoute == Routes.QUIT_CONFIRM || currentRoute == Routes.RHYTHM_MISSION) {
            return@LaunchedEffect
        }
        if (currentRoute !in FocusSessionNav.escapeReactRoutes) return@LaunchedEffect

        if (isHandNear) {
            FocusHandWarningState.entryCause = HandWarningCause.HandNear
        } else if (FocusHandWarningState.entryCause != HandWarningCause.HandNear) {
            FocusHandWarningState.entryCause = HandWarningCause.EscapeAttempt
        }

        if (currentRoute != Routes.HAND_WARNING) {
            navController.navigate(Routes.HAND_WARNING) {
                launchSingleTop = true
            }
        }
    }
}

/** 손이 가까우면 경고 화면, 멀어지면 기본 집중(눈) 화면으로 복귀한다. */
@Composable
fun FocusHandApproachNavigation(
    navController: NavController,
    currentRoute: String?,
    currentFocusSession: WoliFocusSession?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isHandNear by WoliCameraHandApproachCenter.isHandNear.collectAsState()
    var cameraGranted by remember {
        mutableStateOf(CameraHandApproachAccess.isGranted(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraGranted = CameraHandApproachAccess.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val detectionSupported = WoliCameraHandApproachCenter.isDetectionSupported()
    val detectionEnabled = currentFocusSession != null && cameraGranted && detectionSupported

    LaunchedEffect(detectionEnabled, currentFocusSession?.id) {
        if (!detectionEnabled) {
            WoliCameraHandApproachCenter.stopDetection()
            return@LaunchedEffect
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                WoliCameraHandApproachCenter.startDetection(context, lifecycleOwner)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        WoliCameraHandApproachCenter.startDetection(context, lifecycleOwner)

        try {
            awaitCancellation()
        } finally {
            lifecycleOwner.lifecycle.removeObserver(observer)
            WoliCameraHandApproachCenter.stopDetection()
        }
    }

    val handNearNavGate = remember(currentFocusSession?.id) {
        WoliHandWarningEdgeGate(isHandNear)
    }
    var handNavGraceElapsed by remember(currentFocusSession?.id) {
        mutableStateOf(false)
    }

    LaunchedEffect(currentFocusSession?.id) {
        handNavGraceElapsed = false
        delay(HAND_APPROACH_NAV_GRACE_MS)
        handNavGraceElapsed = true
    }

    LaunchedEffect(isHandNear, currentRoute, currentFocusSession?.id, handNavGraceElapsed) {
        if (currentFocusSession == null) return@LaunchedEffect
        if (currentRoute == Routes.QUIT_CONFIRM || currentRoute == Routes.RHYTHM_MISSION) {
            return@LaunchedEffect
        }

        if (isHandNear) {
            if (!handNavGraceElapsed || !handNearNavGate.onHandStatus(isHandNear)) {
                return@LaunchedEffect
            }
            FocusHandWarningState.entryCause = HandWarningCause.HandNear
            if (currentRoute != Routes.HAND_WARNING) {
                navController.navigate(Routes.HAND_WARNING) {
                    launchSingleTop = true
                }
            }
        } else {
            handNearNavGate.onHandStatus(false)
            if (currentRoute != Routes.HAND_WARNING) return@LaunchedEffect
            val returnDelayMs = handWarningReturnDelayMs(FocusHandWarningState.entryCause)
            delay(returnDelayMs)
            if (WoliCameraHandApproachCenter.isHandNear.value) return@LaunchedEffect
            if (navController.currentBackStackEntry?.destination?.route != Routes.HAND_WARNING) {
                return@LaunchedEffect
            }
            FocusHandWarningState.entryCause = null
            navController.returnToFocusEyes()
        }
    }
}

/** 알림·통화 직후에만 STT를 켜고, 기본 집중 화면(눈)으로 돌아오면 끈다. */
@Composable
fun FocusVoiceInterruptHandler(
    navController: NavController,
    currentRoute: String?,
    currentFocusSession: WoliFocusSession?,
) {
    val context = LocalContext.current
    val events by WoliNotificationCenter.events.collectAsState()
    val savedConfig by WoliFocusSessionController.config.collectAsState()
    val focusConfig = currentFocusSession?.config ?: savedConfig
    val listeningRequested by FocusVoiceInterruptState.listeningRequested.collectAsState()
    val latestEvent = events.firstOrNull { event ->
        !focusConfig.allowImportantOnly || event.priority != WoliNotificationPriority.Normal
    }
    val ttsSpeaker = remember(context) { WoliTtsSpeaker(context) }
    val speechRecognizer = remember(context) { WoliSpeechRecognizer(context) }
    val bleClient = remember(context) { WoliBleDeviceClient(context) }
    var replyDraft by remember { mutableStateOf<WoliReplyDraft?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isCommandListening by remember { mutableStateOf(false) }
    var microphonePermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val voiceInterruptActive = currentFocusSession != null && when (currentRoute) {
        Routes.IMPORTANT_CALL -> true
        Routes.FOCUS_EYES -> listeningRequested
        else -> false
    }

    LaunchedEffect(context) {
        WoliReplyHistoryStore.load(context)
    }

    LaunchedEffect(voiceInterruptActive) {
        if (!voiceInterruptActive) {
            FocusVoiceInterruptState.stopListening()
            speechRecognizer.cancel()
            replyDraft = null
            isListening = false
            isCommandListening = false
        }
    }

    DisposableEffect(ttsSpeaker, speechRecognizer) {
        onDispose {
            ttsSpeaker.shutdown()
            speechRecognizer.shutdown()
        }
    }

    fun beginVoiceReply(event: WoliNotificationEvent) {
        if (!event.canReply || !microphonePermissionGranted) return
        ttsSpeaker.stop()
        replyDraft = null
        isListening = true
        speechRecognizer.startListening(
            onResult = { spokenText ->
                replyDraft = WoliReplyDraft(eventId = event.id, replyText = spokenText)
                isListening = false
                FocusVoiceInterruptState.requestListening()
            },
            onError = {
                isListening = false
                FocusVoiceInterruptState.requestListening()
            },
        )
    }

    fun cancelReplyDraft() {
        speechRecognizer.cancel()
        replyDraft = null
        isListening = false
    }

    fun confirmReplyDraft(): WoliReplySendResult? {
        val draft = replyDraft ?: return null
        val draftEvent = events.firstOrNull { it.id == draft.eventId }
        val result = WoliRemoteReplyActionStore.sendReply(
            context = context,
            eventId = draft.eventId,
            replyText = draft.replyText,
        )
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
        }
        return result
    }

    fun completeFocusNormally() {
        if (WoliFocusGuardService.completeNormally(context, bleClient)) {
            navController.navigate(Routes.FOCUS_COMPLETE) {
                popUpTo(FocusSessionNav.POP_UP_TO_ON_SESSION_END) {
                    inclusive = FocusSessionNav.POP_INCLUSIVE_ON_SESSION_END
                }
                launchSingleTop = true
            }
        }
    }

    fun handleVoiceCommand(spokenText: String) {
        val command = WoliVoiceCommandParser.parse(spokenText)
        when (command.type) {
            WoliVoiceCommandType.StartReply -> {
                val event = latestEvent
                if (event == null) {
                    ttsSpeaker.speak("답장할 알림이 없습니다.")
                } else {
                    ttsSpeaker.speak("답장 내용을 말해주세요.", onDone = { beginVoiceReply(event) })
                }
            }
            WoliVoiceCommandType.SendReply -> {
                val result = confirmReplyDraft()
                ttsSpeaker.speak(result?.userMessage() ?: "전송할 답장 초안이 없습니다.")
            }
            WoliVoiceCommandType.CancelReply -> {
                cancelReplyDraft()
                ttsSpeaker.speak("답장 초안을 취소했습니다.")
            }
            WoliVoiceCommandType.AnswerCall -> {
                val result = WoliCallActionController.answer(context)
                ttsSpeaker.speak(result.userMessage())
                if (result.isSuccess) {
                    keepFocusUiDuringActiveCall(context)
                }
            }
            WoliVoiceCommandType.DeclineCall -> {
                val result = WoliCallActionController.decline(context)
                ttsSpeaker.speak(result.userMessage())
                FocusVoiceInterruptState.stopListening()
                return
            }
            WoliVoiceCommandType.ReadRemainingTime -> {
                val remaining = currentFocusSession?.remainingMillis(System.currentTimeMillis()) ?: 0L
                ttsSpeaker.speak("남은 시간은 ${remaining.formatDurationKorean()}입니다.")
            }
            WoliVoiceCommandType.ContinueFocus -> {
                ttsSpeaker.speak("집중을 계속합니다.")
                FocusVoiceInterruptState.stopListening()
                navController.returnToFocusEyes()
                return
            }
            WoliVoiceCommandType.CompleteFocus -> {
                ttsSpeaker.speak("집중을 완료합니다.", onDone = { completeFocusNormally() })
                FocusVoiceInterruptState.stopListening()
                return
            }
            WoliVoiceCommandType.StartUnlockMission -> {
                ttsSpeaker.speak("잠금 해제 미션으로 이동합니다.", onDone = {
                    navController.navigate(Routes.QUIT_CONFIRM) { launchSingleTop = true }
                })
                FocusVoiceInterruptState.stopListening()
                return
            }
            WoliVoiceCommandType.Unknown -> {
                ttsSpeaker.speak(
                    "명령을 이해하지 못했어요. 답장, 보내, 취소, 전화 받아, 거절, 남은 시간 중 하나로 말해주세요.",
                )
            }
        }
        FocusVoiceInterruptState.requestListening()
    }

    fun beginVoiceCommand() {
        if (!voiceInterruptActive || isListening || isCommandListening) return
        if (!microphonePermissionGranted) return

        ttsSpeaker.stop()
        isCommandListening = true
        speechRecognizer.startListening(
            onResult = { spokenText ->
                isCommandListening = false
                handleVoiceCommand(spokenText)
            },
            onError = {
                isCommandListening = false
                if (voiceInterruptActive) {
                    FocusVoiceInterruptState.requestListening()
                }
            },
        )
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphonePermissionGranted = granted
        if (granted && voiceInterruptActive) {
            beginVoiceCommand()
        }
    }

    LaunchedEffect(listeningRequested, voiceInterruptActive, microphonePermissionGranted) {
        if (!listeningRequested || !voiceInterruptActive) return@LaunchedEffect
        if (!microphonePermissionGranted) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return@LaunchedEffect
        }
        if (!isListening && !isCommandListening) {
            beginVoiceCommand()
        }
    }
}

/** 중요 연락 수신·종료·통화 중 집중 화면 복귀를 자동으로 처리한다. */
@Composable
fun FocusCallNavigation(
    navController: NavController,
    currentRoute: String?,
    currentFocusSession: WoliFocusSession?,
) {
    val context = LocalContext.current
    val currentCall by WoliCallCenter.current.collectAsState()
    val savedConfig by WoliFocusSessionController.config.collectAsState()
    val focusConfig = currentFocusSession?.config ?: savedConfig

    val audibleCall = currentCall?.takeIf { call ->
        currentFocusSession != null &&
            (!focusConfig.allowImportantOnly || call.isImportant)
    }

    LaunchedEffect(audibleCall?.id, audibleCall?.state, currentRoute, currentFocusSession?.id) {
        if (currentFocusSession == null) return@LaunchedEffect

        when (audibleCall?.state) {
            WoliCallState.Ringing -> {
                if (currentRoute in FocusSessionNav.callInterruptRoutes &&
                    currentRoute != Routes.IMPORTANT_CALL
                ) {
                    navController.navigate(Routes.IMPORTANT_CALL) {
                        launchSingleTop = true
                    }
                }
            }
            WoliCallState.Active -> {
                keepFocusUiDuringActiveCall(context)
                if (currentRoute == Routes.IMPORTANT_CALL) {
                    navController.returnToFocusEyes()
                }
            }
            WoliCallState.Idle, null -> {
                FocusVoiceInterruptState.stopListening()
                if (currentRoute == Routes.IMPORTANT_CALL) {
                    navController.returnToFocusEyes()
                }
            }
            WoliCallState.Unknown -> Unit
        }
    }
}

fun NavController.returnToFocusEyes() {
    val popped = popBackStack(Routes.FOCUS_EYES, inclusive = false)
    if (!popped) {
        navigate(Routes.FOCUS_EYES) {
            launchSingleTop = true
        }
    }
}

/** 통화를 받은 뒤 시스템 통화 화면 대신 집중 UI를 앞으로 가져온다. */
fun keepFocusUiDuringActiveCall(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_ROUTE, Routes.FOCUS_EYES)
        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    context.startActivity(intent)
}
