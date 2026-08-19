package com.woli.app.notification

import java.util.Locale

object WoliNotificationPolicy {
    private const val CATEGORY_CALL = "call"
    private const val CATEGORY_MESSAGE = "msg"
    private const val CATEGORY_ALARM = "alarm"
    private const val CATEGORY_EVENT = "event"
    private const val CATEGORY_REMINDER = "reminder"

    private val timeSensitiveCategories = setOf(
        CATEGORY_ALARM,
        CATEGORY_EVENT,
        CATEGORY_REMINDER,
    )

    private val urgentKeywords = setOf(
        "긴급",
        "급해",
        "급한",
        "응급",
        "위급",
        "사고",
        "병원",
        "119",
        "경찰",
        "지금 전화",
        "전화해",
        "빨리",
        "asap",
        "emergency",
        "urgent",
        "help",
    )

    private val promotionalKeywords = setOf(
        "광고",
        "광고성",
        "프로모션",
        "할인",
        "쿠폰",
        "세일",
        "특가",
        "혜택",
        "적립",
        "이벤트",
        "무료배송",
        "구매",
        "sale",
        "coupon",
        "discount",
        "promotion",
        "marketing",
        "newsletter",
        "sponsored",
    )

    private val socialEngagementKeywords = setOf(
        "좋아요",
        "댓글",
        "팔로우",
        "팔로워",
        "추천 게시물",
        "새 소식",
        "liked",
        "commented",
        "followed",
        "recommended",
    )

    fun priorityFor(
        category: String?,
        canReply: Boolean,
        importantContactMatched: Boolean = false,
    ): WoliNotificationPriority {
        return evaluate(
            input = WoliNotificationImportanceInput(
                category = category,
                canReply = canReply,
                importantContactMatched = importantContactMatched,
            ),
        ).priority
    }

    fun evaluate(
        input: WoliNotificationImportanceInput,
        config: WoliNotificationRuleConfig = WoliNotificationRuleConfig(),
    ): WoliNotificationImportance {
        val category = input.category?.lowercase(Locale.US)
        val text = normalizedSearchText(
            input.appName,
            input.title,
            input.preview,
            input.packageName,
        )
        val knownMessagePackage = WoliNotificationRuleCatalog.isKnownMessagePackage(input.packageName)
        val messageLike = category == CATEGORY_MESSAGE || input.canReply
        val directMessageLike = knownMessagePackage && messageLike && !input.isGroupConversation
        val urgent = text.hasAny(urgentKeywords + config.extraUrgentKeywords)
        val promotional = text.hasAny(promotionalKeywords)
        val socialEngagement = text.hasAny(socialEngagementKeywords)

        return when {
            category == CATEGORY_CALL -> WoliNotificationImportance.critical(
                reason = WoliNotificationImportanceReason.IncomingCall,
                score = 100,
            )
            config.isBlockedPackage(input.packageName) -> WoliNotificationImportance.normal(
                reason = WoliNotificationImportanceReason.BlockedApp,
                score = 0,
            )
            input.isOngoing -> WoliNotificationImportance.normal(
                reason = WoliNotificationImportanceReason.OngoingStatus,
                score = 0,
            )
            input.isGroupSummary -> WoliNotificationImportance.normal(
                reason = WoliNotificationImportanceReason.GroupSummary,
                score = 0,
            )
            input.isGroupConversation -> WoliNotificationImportance.normal(
                reason = WoliNotificationImportanceReason.GroupConversation,
                score = 5,
            )
            promotional -> WoliNotificationImportance.normal(
                reason = WoliNotificationImportanceReason.Promotional,
                score = 0,
            )
            socialEngagement -> WoliNotificationImportance.normal(
                reason = WoliNotificationImportanceReason.SocialEngagement,
                score = 0,
            )
            input.importantContactMatched && urgent -> WoliNotificationImportance.critical(
                reason = WoliNotificationImportanceReason.ImportantContactUrgent,
                score = 100,
            )
            input.importantContactMatched -> WoliNotificationImportance.important(
                reason = WoliNotificationImportanceReason.ImportantContact,
                score = 75,
            )
            category in timeSensitiveCategories -> WoliNotificationImportance.important(
                reason = WoliNotificationImportanceReason.TimeSensitive,
                score = 70,
            )
            urgent && config.mode != WoliNotificationMode.Strict && directMessageLike ->
                WoliNotificationImportance.critical(
                    reason = WoliNotificationImportanceReason.UrgentKeyword,
                    score = 90,
                )
            config.allowsUserAllowedApps() &&
                directMessageLike &&
                config.isAllowedPackage(input.packageName) ->
                WoliNotificationImportance.important(
                    reason = WoliNotificationImportanceReason.UserAllowedApp,
                    score = 65,
                )
            config.allowsDemoReplyableMessages() && directMessageLike ->
                WoliNotificationImportance.important(
                    reason = WoliNotificationImportanceReason.DemoReplyableMessage,
                    score = 55,
                )
            messageLike || knownMessagePackage -> WoliNotificationImportance.normal(
                reason = WoliNotificationImportanceReason.GeneralMessage,
                score = 20,
            )
            else -> WoliNotificationImportance.normal(
                reason = WoliNotificationImportanceReason.Default,
                score = 10,
            )
        }
    }

    fun safePreview(text: CharSequence?, maxLength: Int = 90): String {
        val normalized = text
            ?.toString()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        if (normalized.length <= maxLength) return normalized
        return normalized.take(maxLength - 1) + "…"
    }

    private fun normalizedSearchText(vararg values: String): String {
        return values
            .joinToString(separator = " ")
            .lowercase(Locale.KOREAN)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.hasAny(keywords: Set<String>): Boolean {
        return keywords
            .map { it.lowercase(Locale.KOREAN).trim() }
            .filter { it.isNotBlank() }
            .any(::contains)
    }
}

data class WoliNotificationImportanceInput(
    val category: String?,
    val canReply: Boolean,
    val importantContactMatched: Boolean = false,
    val title: String = "",
    val preview: String = "",
    val appName: String = "",
    val packageName: String = "",
    val isOngoing: Boolean = false,
    val isGroupSummary: Boolean = false,
    val isGroupConversation: Boolean = false,
    val systemPriority: Int = WoliSystemNotificationPriority.Default,
)

data class WoliNotificationImportance(
    val priority: WoliNotificationPriority,
    val score: Int,
    val reason: WoliNotificationImportanceReason,
) {
    companion object {
        fun critical(
            reason: WoliNotificationImportanceReason,
            score: Int,
        ) = WoliNotificationImportance(
            priority = WoliNotificationPriority.Critical,
            score = score.coerceIn(0, 100),
            reason = reason,
        )

        fun important(
            reason: WoliNotificationImportanceReason,
            score: Int,
        ) = WoliNotificationImportance(
            priority = WoliNotificationPriority.Important,
            score = score.coerceIn(0, 100),
            reason = reason,
        )

        fun normal(
            reason: WoliNotificationImportanceReason,
            score: Int,
        ) = WoliNotificationImportance(
            priority = WoliNotificationPriority.Normal,
            score = score.coerceIn(0, 100),
            reason = reason,
        )
    }
}

enum class WoliNotificationImportanceReason(val label: String) {
    IncomingCall("전화 알림"),
    ImportantContact("중요 연락처"),
    ImportantContactUrgent("중요 연락처 긴급 표현"),
    UrgentKeyword("긴급 표현 포함"),
    TimeSensitive("일정/알람성 알림"),
    UserAllowedApp("사용자 허용 앱"),
    DemoReplyableMessage("시연용 답장 가능 메시지"),
    GeneralMessage("일반 메신저 알림"),
    Promotional("광고/혜택성 알림"),
    SocialEngagement("소셜 반응 알림"),
    GroupSummary("묶음 요약 알림"),
    GroupConversation("단체 대화 알림"),
    OngoingStatus("상태 유지 알림"),
    BlockedApp("사용자 차단 앱"),
    Default("기본 알림"),
}

object WoliSystemNotificationPriority {
    const val Min = -2
    const val Low = -1
    const val Default = 0
    const val High = 1
    const val Max = 2
}
