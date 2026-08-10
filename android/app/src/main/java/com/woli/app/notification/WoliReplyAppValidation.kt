package com.woli.app.notification

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

data class WoliReplyAppTarget(
    val id: String,
    val displayName: String,
    val packageNames: List<String>,
    val testPrompt: String,
)

data class WoliReplyAppValidationStatus(
    val target: WoliReplyAppTarget,
    val installedPackageName: String?,
    val notificationCount: Int,
    val replyActionCount: Int,
    val sentCount: Int,
    val failedCount: Int,
    val latestEventTitle: String?,
    val latestResultMessage: String?,
) {
    val stage: WoliReplyAppValidationStage
        get() = when {
            sentCount > 0 -> WoliReplyAppValidationStage.Ready
            replyActionCount > 0 -> WoliReplyAppValidationStage.ReplyActionObserved
            notificationCount > 0 -> WoliReplyAppValidationStage.NotificationObserved
            installedPackageName != null -> WoliReplyAppValidationStage.Installed
            else -> WoliReplyAppValidationStage.NotInstalled
        }

    val needsAttention: Boolean
        get() = failedCount > 0 || stage != WoliReplyAppValidationStage.Ready
}

enum class WoliReplyAppValidationStage {
    Ready,
    ReplyActionObserved,
    NotificationObserved,
    Installed,
    NotInstalled,
}

object WoliReplyAppValidationCatalog {
    val targets = listOf(
        WoliReplyAppTarget(
            id = "kakao",
            displayName = "카카오톡",
            packageNames = listOf("com.kakao.talk"),
            testPrompt = "개인 채팅방에서 짧은 메시지 알림을 보낸 뒤 음성 답장을 전송하세요.",
        ),
        WoliReplyAppTarget(
            id = "sms",
            displayName = "문자 메시지",
            packageNames = listOf(
                "com.google.android.apps.messaging",
                "com.samsung.android.messaging",
            ),
            testPrompt = "기본 문자 앱으로 SMS/RCS 알림을 보낸 뒤 답장 액션을 확인하세요.",
        ),
        WoliReplyAppTarget(
            id = "telegram",
            displayName = "Telegram",
            packageNames = listOf(
                "org.telegram.messenger",
                "org.thunderdog.challegram",
            ),
            testPrompt = "테스트 채팅방에서 메시지를 보내고 알림 답장 액션을 확인하세요.",
        ),
        WoliReplyAppTarget(
            id = "whatsapp",
            displayName = "WhatsApp",
            packageNames = listOf("com.whatsapp"),
            testPrompt = "개인 채팅 알림으로 음성 답장 전송까지 확인하세요.",
        ),
    )

    fun installedKnownPackages(context: Context): Set<String> {
        return targets
            .flatMap { it.packageNames }
            .filter { packageName -> context.packageManager.hasPackage(packageName) }
            .toSet()
    }

    fun buildStatuses(
        installedPackages: Set<String>,
        events: List<WoliNotificationEvent>,
        history: List<WoliReplyHistoryEntry>,
    ): List<WoliReplyAppValidationStatus> {
        return targets.map { target ->
            val targetEvents = events.filter { it.packageName in target.packageNames }
            val targetHistory = history.filter { it.packageName in target.packageNames }
            val latestHistory = targetHistory.maxByOrNull { it.createdAtMillis }
            val latestEvent = targetEvents.maxByOrNull { it.postedAtMillis }

            WoliReplyAppValidationStatus(
                target = target,
                installedPackageName = target.packageNames.firstOrNull { it in installedPackages },
                notificationCount = targetEvents.size,
                replyActionCount = targetEvents.count { it.canReply },
                sentCount = targetHistory.count { it.resultType == WoliReplyHistoryResultType.Sent },
                failedCount = targetHistory.count { it.resultType != WoliReplyHistoryResultType.Sent },
                latestEventTitle = latestHistory?.title ?: latestEvent?.title,
                latestResultMessage = latestHistory?.resultMessage,
            )
        }
    }

    fun stageLabel(stage: WoliReplyAppValidationStage): String {
        return when (stage) {
            WoliReplyAppValidationStage.Ready -> "검증 완료"
            WoliReplyAppValidationStage.ReplyActionObserved -> "전송 테스트 필요"
            WoliReplyAppValidationStage.NotificationObserved -> "답장 액션 미확인"
            WoliReplyAppValidationStage.Installed -> "알림 대기"
            WoliReplyAppValidationStage.NotInstalled -> "미설치"
        }
    }

    private fun PackageManager.hasPackage(packageName: String): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                getPackageInfo(packageName, 0)
            }
        }.isSuccess
    }
}
