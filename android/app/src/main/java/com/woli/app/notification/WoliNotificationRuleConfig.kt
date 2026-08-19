package com.woli.app.notification

data class WoliNotificationRuleConfig(
    val mode: WoliNotificationMode = WoliNotificationMode.Balanced,
    val allowedPackageNames: Set<String> = emptySet(),
    val blockedPackageNames: Set<String> = emptySet(),
    val extraUrgentKeywords: Set<String> = emptySet(),
) {
    fun isAllowedPackage(packageName: String): Boolean {
        return packageName in allowedPackageNames
    }

    fun isBlockedPackage(packageName: String): Boolean {
        return packageName in blockedPackageNames
    }

    fun allowsUserAllowedApps(): Boolean {
        return mode != WoliNotificationMode.Strict
    }

    fun allowsDemoReplyableMessages(): Boolean {
        return mode == WoliNotificationMode.Demo
    }
}

enum class WoliNotificationMode(
    val label: String,
    val summary: String,
) {
    Strict(
        label = "집중 우선",
        summary = "전화, 중요 연락처, 알람만 안내",
    ),
    Balanced(
        label = "균형",
        summary = "중요 연락처, 긴급 표현, 허용 앱만 안내",
    ),
    Demo(
        label = "시연",
        summary = "답장 가능한 테스트 메시지도 안내",
    ),
}

data class WoliNotificationAllowedAppTarget(
    val id: String,
    val displayName: String,
    val packageNames: Set<String>,
)

object WoliNotificationRuleCatalog {
    val allowedAppTargets = listOf(
        WoliNotificationAllowedAppTarget(
            id = "kakao",
            displayName = "카카오톡",
            packageNames = setOf("com.kakao.talk"),
        ),
        WoliNotificationAllowedAppTarget(
            id = "sms",
            displayName = "문자 메시지",
            packageNames = setOf(
                "com.google.android.apps.messaging",
                "com.samsung.android.messaging",
            ),
        ),
        WoliNotificationAllowedAppTarget(
            id = "telegram",
            displayName = "Telegram",
            packageNames = setOf(
                "org.telegram.messenger",
                "org.thunderdog.challegram",
            ),
        ),
        WoliNotificationAllowedAppTarget(
            id = "whatsapp",
            displayName = "WhatsApp",
            packageNames = setOf("com.whatsapp"),
        ),
        WoliNotificationAllowedAppTarget(
            id = "line",
            displayName = "LINE",
            packageNames = setOf("jp.naver.line.android"),
        ),
        WoliNotificationAllowedAppTarget(
            id = "instagram",
            displayName = "Instagram DM",
            packageNames = setOf("com.instagram.android"),
        ),
        WoliNotificationAllowedAppTarget(
            id = "messenger",
            displayName = "Messenger",
            packageNames = setOf("com.facebook.orca"),
        ),
    )

    private val messagePackageNames = allowedAppTargets
        .flatMap { it.packageNames }
        .toSet()

    fun isKnownMessagePackage(packageName: String): Boolean {
        return packageName in messagePackageNames
    }

    fun targetById(id: String): WoliNotificationAllowedAppTarget? {
        return allowedAppTargets.firstOrNull { it.id == id }
    }
}
