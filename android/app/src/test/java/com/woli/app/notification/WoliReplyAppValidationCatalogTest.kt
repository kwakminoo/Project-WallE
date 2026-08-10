package com.woli.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WoliReplyAppValidationCatalogTest {
    @Test
    fun installedPackageWithoutNotificationStaysInstalled() {
        val statuses = WoliReplyAppValidationCatalog.buildStatuses(
            installedPackages = setOf("com.kakao.talk"),
            events = emptyList(),
            history = emptyList(),
        )

        val kakao = statuses.first { it.target.id == "kakao" }

        assertEquals(WoliReplyAppValidationStage.Installed, kakao.stage)
        assertEquals("com.kakao.talk", kakao.installedPackageName)
        assertTrue(kakao.needsAttention)
    }

    @Test
    fun replyableNotificationRequiresSendTest() {
        val statuses = WoliReplyAppValidationCatalog.buildStatuses(
            installedPackages = setOf("com.kakao.talk"),
            events = listOf(
                notificationEvent(
                    packageName = "com.kakao.talk",
                    canReply = true,
                ),
            ),
            history = emptyList(),
        )

        val kakao = statuses.first { it.target.id == "kakao" }

        assertEquals(WoliReplyAppValidationStage.ReplyActionObserved, kakao.stage)
        assertEquals(1, kakao.notificationCount)
        assertEquals(1, kakao.replyActionCount)
    }

    @Test
    fun sentHistoryMarksTargetReady() {
        val statuses = WoliReplyAppValidationCatalog.buildStatuses(
            installedPackages = setOf("com.google.android.apps.messaging"),
            events = listOf(
                notificationEvent(
                    packageName = "com.google.android.apps.messaging",
                    canReply = true,
                ),
            ),
            history = listOf(
                historyEntry(
                    packageName = "com.google.android.apps.messaging",
                    resultType = WoliReplyHistoryResultType.Sent,
                ),
            ),
        )

        val sms = statuses.first { it.target.id == "sms" }

        assertEquals(WoliReplyAppValidationStage.Ready, sms.stage)
        assertEquals(1, sms.sentCount)
        assertEquals(false, sms.needsAttention)
    }

    @Test
    fun failedHistoryKeepsAttentionEvenAfterReady() {
        val statuses = WoliReplyAppValidationCatalog.buildStatuses(
            installedPackages = setOf("com.whatsapp"),
            events = emptyList(),
            history = listOf(
                historyEntry(
                    packageName = "com.whatsapp",
                    resultType = WoliReplyHistoryResultType.Sent,
                    createdAtMillis = 1_000L,
                ),
                historyEntry(
                    packageName = "com.whatsapp",
                    resultType = WoliReplyHistoryResultType.Expired,
                    resultMessage = "만료",
                    createdAtMillis = 2_000L,
                ),
            ),
        )

        val whatsapp = statuses.first { it.target.id == "whatsapp" }

        assertEquals(WoliReplyAppValidationStage.Ready, whatsapp.stage)
        assertEquals(1, whatsapp.sentCount)
        assertEquals(1, whatsapp.failedCount)
        assertEquals("만료", whatsapp.latestResultMessage)
        assertTrue(whatsapp.needsAttention)
    }

    @Test
    fun stageLabelsAreKoreanUserFacingText() {
        assertEquals(
            "검증 완료",
            WoliReplyAppValidationCatalog.stageLabel(WoliReplyAppValidationStage.Ready),
        )
        assertEquals(
            "알림 대기",
            WoliReplyAppValidationCatalog.stageLabel(WoliReplyAppValidationStage.Installed),
        )
    }

    private fun notificationEvent(
        packageName: String,
        canReply: Boolean,
    ) = WoliNotificationEvent(
        id = "${packageName}_event",
        packageName = packageName,
        appName = packageName,
        title = "테스트 알림",
        preview = "답장 테스트",
        priority = WoliNotificationPriority.Important,
        canReply = canReply,
        postedAtMillis = 1_000L,
    )

    private fun historyEntry(
        packageName: String,
        resultType: WoliReplyHistoryResultType,
        resultMessage: String = "완료",
        createdAtMillis: Long = 1_000L,
    ) = WoliReplyHistoryEntry(
        id = "${packageName}_history_$createdAtMillis",
        eventId = "${packageName}_event",
        packageName = packageName,
        appName = packageName,
        title = "테스트 알림",
        preview = "답장 테스트",
        replyText = "확인했어",
        resultType = resultType,
        resultMessage = resultMessage,
        createdAtMillis = createdAtMillis,
    )
}
