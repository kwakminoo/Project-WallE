@file:Suppress("DEPRECATION")

package com.woli.app.call

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.woli.app.contacts.WoliContactsAccess
import com.woli.app.contacts.WoliImportantContactMatcher
import com.woli.app.contacts.WoliImportantContactsStore
import com.woli.app.contacts.WoliPhoneNumberNormalizer

class WoliCallMonitor(
    private val context: Context,
    private val onStateChanged: (WoliCallState, WoliCallerInfo?) -> Unit,
) {
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null

    @SuppressLint("MissingPermission")
    fun start(): WoliCallMonitorStartResult {
        if (!WoliCallAccess.isGranted(context)) {
            return WoliCallMonitorStartResult.MissingPermission
        }

        val manager = context.getSystemService(TelephonyManager::class.java)
            ?: return WoliCallMonitorStartResult.Unavailable

        stop()
        WoliImportantContactsStore.load(context)

        val listener = object : PhoneStateListener(context.mainExecutor) {
            @Deprecated("Deprecated callback is still the only public callback exposing best-effort caller number.")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                onStateChanged(
                    WoliCallStateMapper.fromTelephonyState(state),
                    callerInfoFor(phoneNumber),
                )
            }
        }

        return runCatching {
            telephonyManager = manager
            phoneStateListener = listener
            manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            onStateChanged(
                WoliCallStateMapper.fromTelephonyState(manager.callStateForSubscription),
                null,
            )
            WoliCallMonitorStartResult.Started
        }.getOrElse { error ->
            phoneStateListener = null
            telephonyManager = null
            WoliCallMonitorStartResult.Failed(
                error.message ?: "전화 상태 감지를 시작하지 못했습니다.",
            )
        }
    }

    fun stop() {
        val listener = phoneStateListener ?: return
        runCatching {
            telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
        telephonyManager = null
    }

    private fun callerInfoFor(phoneNumber: String?): WoliCallerInfo? {
        val normalizedPhoneNumber = WoliPhoneNumberNormalizer.normalize(phoneNumber)
        if (normalizedPhoneNumber.isBlank()) return null

        val contact = WoliImportantContactMatcher.matchPhoneNumber(
            phoneNumber = normalizedPhoneNumber,
            contacts = WoliImportantContactsStore.enabledContactsSnapshot(),
        )
        val label = contact?.displayName
            ?: if (WoliContactsAccess.canReadCallerId(context)) {
                WoliPhoneNumberNormalizer.mask(normalizedPhoneNumber)
            } else {
                WoliCallEvent.UNKNOWN_CALLER_LABEL
            }

        return WoliCallerInfo(
            label = label,
            phoneNumber = normalizedPhoneNumber,
            isImportant = contact != null,
        )
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
