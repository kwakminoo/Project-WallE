package com.woli.app.call

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

class WoliCallMonitor(
    private val context: Context,
    private val onStateChanged: (WoliCallState) -> Unit,
) {
    private var telephonyManager: TelephonyManager? = null
    private var callback: TelephonyCallback? = null

    @SuppressLint("MissingPermission")
    fun start(): WoliCallMonitorStartResult {
        if (!WoliCallAccess.isGranted(context)) {
            return WoliCallMonitorStartResult.MissingPermission
        }

        val manager = context.getSystemService(TelephonyManager::class.java)
            ?: return WoliCallMonitorStartResult.Unavailable

        stop()

        val callStateCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                onStateChanged(WoliCallStateMapper.fromTelephonyState(state))
            }
        }

        return runCatching {
            telephonyManager = manager
            callback = callStateCallback
            manager.registerTelephonyCallback(context.mainExecutor, callStateCallback)
            onStateChanged(WoliCallStateMapper.fromTelephonyState(manager.callStateForSubscription))
            WoliCallMonitorStartResult.Started
        }.getOrElse { error ->
            callback = null
            telephonyManager = null
            WoliCallMonitorStartResult.Failed(
                error.message ?: "전화 상태 감지를 시작하지 못했습니다.",
            )
        }
    }

    fun stop() {
        val currentCallback = callback ?: return
        runCatching {
            telephonyManager?.unregisterTelephonyCallback(currentCallback)
        }
        callback = null
        telephonyManager = null
    }
}

sealed class WoliCallMonitorStartResult {
    data object Started : WoliCallMonitorStartResult()
    data object MissingPermission : WoliCallMonitorStartResult()
    data object Unavailable : WoliCallMonitorStartResult()
    data class Failed(val reason: String) : WoliCallMonitorStartResult()

    val isStarted: Boolean
        get() = this is Started

    fun userMessage(): String {
        return when (this) {
            Started -> "전화 상태 감지를 시작했어요."
            MissingPermission -> "전화 상태 권한이 있어야 수신 전화를 감지할 수 있습니다."
            Unavailable -> "이 기기에서는 전화 상태 감지를 사용할 수 없습니다."
            is Failed -> reason
        }
    }
}
