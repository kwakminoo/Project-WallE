package com.woli.app.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class WoliNotificationPolicyTest {
    @Test
    fun callCategoryIsCritical() {
        val importance = WoliNotificationPolicy.evaluate(
            WoliNotificationImportanceInput(
                category = "call",
                canReply = false,
            ),
        )

        assertEquals(WoliNotificationPriority.Critical, importance.priority)
        assertEquals(WoliNotificationImportanceReason.IncomingCall, importance.reason)
    }

    @Test
    fun normalMessengerMessageIsIgnoredByDefault() {
        val importance = WoliNotificationPolicy.evaluate(
            messageInput(
                packageName = "com.kakao.talk",
                title = "친구",
                preview = "ㅋㅋ 지금 뭐해",
                canReply = true,
            ),
        )

        assertEquals(WoliNotificationPriority.Normal, importance.priority)
        assertEquals(WoliNotificationImportanceReason.GeneralMessage, importance.reason)
    }

    @Test
    fun importantContactMessageIsImportant() {
        val importance = WoliNotificationPolicy.evaluate(
            messageInput(
                packageName = "com.kakao.talk",
                title = "어머니",
                preview = "도착하면 연락줘",
                canReply = true,
                importantContactMatched = true,
            ),
        )

        assertEquals(WoliNotificationPriority.Important, importance.priority)
        assertEquals(WoliNotificationImportanceReason.ImportantContact, importance.reason)
    }

    @Test
    fun importantContactUrgentMessageIsCritical() {
        val importance = WoliNotificationPolicy.evaluate(
            messageInput(
                packageName = "com.kakao.talk",
                title = "어머니",
                preview = "긴급이야 지금 전화해줘",
                canReply = true,
                importantContactMatched = true,
            ),
        )

        assertEquals(WoliNotificationPriority.Critical, importance.priority)
        assertEquals(WoliNotificationImportanceReason.ImportantContactUrgent, importance.reason)
    }

    @Test
    fun urgentKnownMessengerMessageIsCriticalInBalancedMode() {
        val importance = WoliNotificationPolicy.evaluate(
            input = messageInput(
                packageName = "com.kakao.talk",
                title = "팀원",
                preview = "긴급 회의야 빨리 확인해줘",
                canReply = true,
            ),
            config = WoliNotificationRuleConfig(mode = WoliNotificationMode.Balanced),
        )

        assertEquals(WoliNotificationPriority.Critical, importance.priority)
        assertEquals(WoliNotificationImportanceReason.UrgentKeyword, importance.reason)
    }

    @Test
    fun urgentKnownMessengerMessageIsIgnoredInStrictMode() {
        val importance = WoliNotificationPolicy.evaluate(
            input = messageInput(
                packageName = "com.kakao.talk",
                title = "팀원",
                preview = "긴급 회의야 빨리 확인해줘",
                canReply = true,
            ),
            config = WoliNotificationRuleConfig(mode = WoliNotificationMode.Strict),
        )

        assertEquals(WoliNotificationPriority.Normal, importance.priority)
        assertEquals(WoliNotificationImportanceReason.GeneralMessage, importance.reason)
    }

    @Test
    fun allowedAppDirectMessageIsImportantInBalancedMode() {
        val importance = WoliNotificationPolicy.evaluate(
            input = messageInput(
                packageName = "com.kakao.talk",
                title = "친구",
                preview = "테스트 메시지",
                canReply = true,
            ),
            config = WoliNotificationRuleConfig(
                mode = WoliNotificationMode.Balanced,
                allowedPackageNames = setOf("com.kakao.talk"),
            ),
        )

        assertEquals(WoliNotificationPriority.Important, importance.priority)
        assertEquals(WoliNotificationImportanceReason.UserAllowedApp, importance.reason)
    }

    @Test
    fun allowedAppDirectMessageIsIgnoredInStrictMode() {
        val importance = WoliNotificationPolicy.evaluate(
            input = messageInput(
                packageName = "com.kakao.talk",
                title = "친구",
                preview = "테스트 메시지",
                canReply = true,
            ),
            config = WoliNotificationRuleConfig(
                mode = WoliNotificationMode.Strict,
                allowedPackageNames = setOf("com.kakao.talk"),
            ),
        )

        assertEquals(WoliNotificationPriority.Normal, importance.priority)
        assertEquals(WoliNotificationImportanceReason.GeneralMessage, importance.reason)
    }

    @Test
    fun demoModeAllowsReplyableKnownMessengerMessage() {
        val importance = WoliNotificationPolicy.evaluate(
            input = messageInput(
                packageName = "com.kakao.talk",
                title = "테스트",
                preview = "시연 메시지",
                canReply = true,
            ),
            config = WoliNotificationRuleConfig(mode = WoliNotificationMode.Demo),
        )

        assertEquals(WoliNotificationPriority.Important, importance.priority)
        assertEquals(WoliNotificationImportanceReason.DemoReplyableMessage, importance.reason)
    }

    @Test
    fun promotionalMessageIsAlwaysFilteredAsNormal() {
        val importance = WoliNotificationPolicy.evaluate(
            input = messageInput(
                packageName = "com.kakao.talk",
                title = "쇼핑몰",
                preview = "[광고] 오늘만 쿠폰 할인 혜택",
                canReply = true,
                importantContactMatched = true,
            ),
            config = WoliNotificationRuleConfig(
                mode = WoliNotificationMode.Demo,
                allowedPackageNames = setOf("com.kakao.talk"),
            ),
        )

        assertEquals(WoliNotificationPriority.Normal, importance.priority)
        assertEquals(WoliNotificationImportanceReason.Promotional, importance.reason)
    }

    @Test
    fun groupSummaryMessageIsFilteredAsNormal() {
        val importance = WoliNotificationPolicy.evaluate(
            messageInput(
                packageName = "com.kakao.talk",
                title = "채팅 알림 5개",
                preview = "새 메시지가 있습니다.",
                canReply = false,
                isGroupSummary = true,
            ),
        )

        assertEquals(WoliNotificationPriority.Normal, importance.priority)
        assertEquals(WoliNotificationImportanceReason.GroupSummary, importance.reason)
    }

    @Test
    fun groupConversationMessageIsFilteredAsNormal() {
        val importance = WoliNotificationPolicy.evaluate(
            messageInput(
                packageName = "com.kakao.talk",
                title = "팀 단체방",
                preview = "긴급 확인 부탁",
                canReply = true,
                isGroupConversation = true,
            ),
        )

        assertEquals(WoliNotificationPriority.Normal, importance.priority)
        assertEquals(WoliNotificationImportanceReason.GroupConversation, importance.reason)
    }

    @Test
    fun ongoingStatusNotificationIsFilteredAsNormal() {
        val importance = WoliNotificationPolicy.evaluate(
            WoliNotificationImportanceInput(
                category = null,
                canReply = true,
                title = "음악 재생 중",
                preview = "현재 재생 중인 콘텐츠",
                isOngoing = true,
            ),
        )

        assertEquals(WoliNotificationPriority.Normal, importance.priority)
        assertEquals(WoliNotificationImportanceReason.OngoingStatus, importance.reason)
    }

    @Test
    fun alarmCategoryIsImportant() {
        val importance = WoliNotificationPolicy.evaluate(
            WoliNotificationImportanceInput(
                category = "alarm",
                canReply = false,
                title = "일정 알림",
                preview = "10분 뒤 회의가 시작됩니다.",
            ),
        )

        assertEquals(WoliNotificationPriority.Important, importance.priority)
        assertEquals(WoliNotificationImportanceReason.TimeSensitive, importance.reason)
    }

    @Test
    fun safePreviewNormalizesWhitespaceAndLimitsLength() {
        val preview = WoliNotificationPolicy.safePreview(
            "  집중   중에\n읽을 긴 메시지입니다. ".repeat(6),
            maxLength = 24,
        )

        assertEquals(24, preview.length)
        assertEquals(true, preview.endsWith("…"))
    }

    private fun messageInput(
        packageName: String,
        title: String,
        preview: String,
        canReply: Boolean,
        importantContactMatched: Boolean = false,
        isGroupSummary: Boolean = false,
        isGroupConversation: Boolean = false,
    ) = WoliNotificationImportanceInput(
        category = "msg",
        canReply = canReply,
        importantContactMatched = importantContactMatched,
        title = title,
        preview = preview,
        appName = "메신저",
        packageName = packageName,
        isGroupSummary = isGroupSummary,
        isGroupConversation = isGroupConversation,
    )
}
