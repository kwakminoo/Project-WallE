package com.woli.app.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.woli.app.device.WoliBleDeviceClient
import com.woli.app.device.WoliDeviceProtocol
import com.woli.app.focus.hand.WoliCameraHandApproachCenter
import com.woli.app.navigation.FocusSessionNav
import kotlinx.coroutines.delay

/** Counts hand-proximity rising edges during an active focus session (BLE + stats). */
@Composable
fun FocusHandApproachMonitor(
    enabled: Boolean,
) {
    val context = LocalContext.current
    val bleClient = remember(context) { WoliBleDeviceClient(context) }
    val isHandNear by WoliCameraHandApproachCenter.isHandNear.collectAsState()
    val currentSession by WoliFocusSessionController.current.collectAsState()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastRelayedHandNear by remember { mutableStateOf<Boolean?>(null) }
    val handWarningGate = remember(currentSession?.id) {
        WoliHandWarningEdgeGate(isHandNear)
    }

    LaunchedEffect(isHandNear, currentSession?.id, enabled) {
        if (!enabled || currentSession == null) {
            lastRelayedHandNear = null
            return@LaunchedEffect
        }

        val previous = lastRelayedHandNear
        if (previous == null) {
            lastRelayedHandNear = isHandNear
            if (isHandNear) {
                bleClient.sendCommand(WoliDeviceProtocol.COMMAND_HAND_NEAR)
            }
            return@LaunchedEffect
        }
        if (previous == isHandNear) return@LaunchedEffect

        bleClient.sendCommand(
            if (isHandNear) {
                WoliDeviceProtocol.COMMAND_HAND_NEAR
            } else {
                WoliDeviceProtocol.COMMAND_HAND_FAR
            },
        )
        lastRelayedHandNear = isHandNear
    }

    if (!enabled) return

    LaunchedEffect(currentSession?.id) {
        while (WoliFocusSessionController.current.value != null) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(isHandNear, currentSession?.id) {
        if (currentSession == null) return@LaunchedEffect
        if (handWarningGate.onHandStatus(isHandNear)) {
            WoliFocusSessionController.addHandWarningIfActive(nowMillis)
        }
    }
}

/** @see FocusSessionNav.handApproachMonitorRoutes */
fun focusHandApproachMonitorEnabled(
    currentRoute: String?,
    currentFocusSession: WoliFocusSession?,
): Boolean {
    return currentFocusSession != null &&
        currentRoute in FocusSessionNav.handApproachMonitorRoutes
}
