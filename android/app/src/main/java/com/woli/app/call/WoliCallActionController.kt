@file:Suppress("DEPRECATION")

package com.woli.app.call

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

object WoliCallActionController {
    fun canControlCalls(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ANSWER_PHONE_CALLS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun answer(context: Context): WoliCallActionResult {
        if (!canControlCalls(context)) return WoliCallActionResult.MissingPermission
        val telecomManager = context.getSystemService(TelecomManager::class.java)
            ?: return WoliCallActionResult.Unavailable

        return runCatching {
            telecomManager.acceptRingingCall()
            WoliCallActionResult.Success("전화 받기 요청을 보냈습니다.")
        }.getOrElse { error ->
            WoliCallActionResult.Failed(error.message ?: "전화 받기 요청이 차단되었습니다.")
        }
    }

    @SuppressLint("MissingPermission")
    fun decline(context: Context): WoliCallActionResult {
        if (!canControlCalls(context)) return WoliCallActionResult.MissingPermission
        val telecomManager = context.getSystemService(TelecomManager::class.java)
            ?: return WoliCallActionResult.Unavailable

        return runCatching {
            if (telecomManager.endCall()) {
                WoliCallActionResult.Success("전화 거절 요청을 보냈습니다.")
            } else {
                WoliCallActionResult.Failed("현재 기기에서 전화 거절 요청이 허용되지 않았습니다.")
            }
        }.getOrElse { error ->
            WoliCallActionResult.Failed(error.message ?: "전화 거절 요청이 차단되었습니다.")
        }
    }

    fun showCallScreen(context: Context): WoliCallActionResult {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return WoliCallActionResult.MissingPermission
        }
        val telecomManager = context.getSystemService(TelecomManager::class.java)
            ?: return WoliCallActionResult.Unavailable

        return runCatching {
            telecomManager.showInCallScreen(false)
            WoliCallActionResult.Success("통화 화면을 열었습니다.")
        }.getOrElse { error ->
            WoliCallActionResult.Failed(error.message ?: "통화 화면을 열지 못했습니다.")
        }
    }
}

sealed class WoliCallActionResult {
    data class Success(val message: String) : WoliCallActionResult()
    data object MissingPermission : WoliCallActionResult()
    data object Unavailable : WoliCallActionResult()
    data class Failed(val reason: String) : WoliCallActionResult()

    val isSuccess: Boolean
        get() = this is Success

    fun userMessage(): String {
        return when (this) {
            is Success -> message
            MissingPermission -> "전화 제어 권한이 필요합니다."
            Unavailable -> "이 기기에서는 전화 제어를 사용할 수 없습니다."
            is Failed -> reason
        }
    }
}
