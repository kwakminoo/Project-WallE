package com.woli.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.call.WoliCallCenter
import com.woli.app.call.WoliCallState
import com.woli.app.device.WoliBleDeviceClient
import com.woli.app.focus.FocusEscapeAttemptCenter
import com.woli.app.focus.WoliFocusEscapeFeedback
import com.woli.app.focus.WoliFocusExitReason
import com.woli.app.focus.WoliFocusGuardService
import com.woli.app.focus.WoliFocusSessionController
import com.woli.app.focus.runHandWarningShake
import com.woli.app.notification.WoliNotificationCenter
import com.woli.app.notification.WoliNotificationPriority
import com.woli.app.notification.WoliReplyHistoryStore
import com.woli.app.ui.components.EyeMood
import com.woli.app.ui.components.WoliEyes
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.components.rememberFocusEyeMetrics
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliOrange
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliWarning
import com.woli.app.ui.theme.WoliYellow
import kotlinx.coroutines.delay

@Composable
fun FocusEyesScreen(
    onQuit: () -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val events by WoliNotificationCenter.events.collectAsState()
    val currentCall by WoliCallCenter.current.collectAsState()
    val currentSession by WoliFocusSessionController.current.collectAsState()
    val savedConfig by WoliFocusSessionController.config.collectAsState()
    val focusConfig = currentSession?.config ?: savedConfig
    val latestEvent = events.firstOrNull { event ->
        !focusConfig.allowImportantOnly || event.priority != WoliNotificationPriority.Normal
    }
    val audibleCall = currentCall?.takeIf { call ->
        !focusConfig.allowImportantOnly || call.isImportant
    }
    val bleClient = remember(context) { WoliBleDeviceClient(context) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var autoCompleted by remember(currentSession?.id) { mutableStateOf(false) }

    LaunchedEffect(currentSession?.id) {
        if (WoliFocusSessionController.current.value == null) {
            if (WoliFocusSessionController.latestCompleted()?.exitReason == WoliFocusExitReason.Completed) {
                onComplete()
            }
            return@LaunchedEffect
        }
        WoliFocusGuardService.start(context)
        while (true) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
            val activeSession = WoliFocusSessionController.current.value ?: return@LaunchedEffect
            if (!autoCompleted && activeSession.remainingMillis(nowMillis) <= 0L) {
                autoCompleted = true
                if (WoliFocusGuardService.completeNormally(context, bleClient, nowMillis)) onComplete()
            }
        }
    }

    val mood = when {
        audibleCall?.state == WoliCallState.Ringing -> EyeMood.Happy
        latestEvent != null -> EyeMood.Alert
        else -> EyeMood.Idle
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack),
        contentAlignment = Alignment.Center,
    ) {
        FocusEyesOnly(mood = mood)
    }
}

@Composable
private fun FocusEyesOnly(mood: EyeMood) {
    val eyeMetrics = rememberFocusEyeMetrics()
    WoliEyes(
        mood = mood,
        modifier = Modifier.fillMaxSize(),
        eyeSize = eyeMetrics.eyeSize,
        gap = eyeMetrics.gap,
    )
}

@Composable
fun RemainingTimeScreen(onBackEyes: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(5_000L)
        onBackEyes()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack),
        contentAlignment = Alignment.Center,
    ) {
        FocusEyesOnly(mood = EyeMood.Idle)
    }
}

@Composable
fun ImportantCallScreen(
    onAnswer: () -> Unit,
    onLater: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack),
        contentAlignment = Alignment.Center,
    ) {
        FocusEyesOnly(mood = EyeMood.Happy)
    }
}

@Composable
fun HandWarningScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val attemptNonce by FocusEscapeAttemptCenter.nonce.collectAsState()
    val shakeX = remember { Animatable(0f) }

    LaunchedEffect(attemptNonce) {
        if (attemptNonce == 0L) return@LaunchedEffect
        WoliFocusEscapeFeedback.playSfx(context)
        shakeX.runHandWarningShake()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = shakeX.value }
            .background(WoliBlack),
        contentAlignment = Alignment.Center,
    ) {
        FocusEyesOnly(mood = EyeMood.Angry)
    }
}

@Composable
fun FocusCompleteScreen(onReport: () -> Unit, onHome: () -> Unit) {
    BackHandler(onBack = onHome)
    LaunchedEffect(Unit) {
        delay(3_000L)
        onReport()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FocusEyesOnly(mood = EyeMood.Complete)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ConfettiHints()
            }
        }
    }
}

@Composable
fun QuitConfirmScreen(onContinue: () -> Unit, onStartMission: () -> Unit) {
    BackHandler(onBack = onContinue)
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
fun SessionReportScreen(onHome: () -> Unit) {
    BackHandler(onBack = onHome)
    val context = LocalContext.current
    LaunchedEffect(context) {
        WoliReplyHistoryStore.load(context)
    }
    LaunchedEffect(Unit) {
        delay(5_000L)
        onHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack),
        contentAlignment = Alignment.Center,
    ) {
        FocusEyesOnly(mood = EyeMood.Complete)
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
