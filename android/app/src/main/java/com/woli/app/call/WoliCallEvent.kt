package com.woli.app.call

import android.telephony.TelephonyManager
import com.woli.app.contacts.WoliPhoneNumberNormalizer

data class WoliCallEvent(
    val id: String,
    val state: WoliCallState,
    val callerLabel: String,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val callerNumber: String? = null,
    val normalizedCallerNumber: String? = callerNumber?.let(WoliPhoneNumberNormalizer::normalize),
    val isImportant: Boolean = false,
) {
    companion object {
        const val UNKNOWN_CALLER_LABEL = "수신 전화"
    }
}

data class WoliCallerInfo(
    val label: String = WoliCallEvent.UNKNOWN_CALLER_LABEL,
    val phoneNumber: String? = null,
    val isImportant: Boolean = false,
)

enum class WoliCallState {
    Ringing,
    Active,
    Idle,
    Unknown,
}

object WoliCallStateMapper {
    fun fromTelephonyState(state: Int): WoliCallState {
        return when (state) {
            TelephonyManager.CALL_STATE_RINGING -> WoliCallState.Ringing
            TelephonyManager.CALL_STATE_OFFHOOK -> WoliCallState.Active
            TelephonyManager.CALL_STATE_IDLE -> WoliCallState.Idle
            else -> WoliCallState.Unknown
        }
    }

    fun displayLabel(state: WoliCallState): String {
        return when (state) {
            WoliCallState.Ringing -> "전화 수신 중"
            WoliCallState.Active -> "통화 중"
            WoliCallState.Idle -> "통화 종료"
            WoliCallState.Unknown -> "상태 확인 중"
        }
    }
}
