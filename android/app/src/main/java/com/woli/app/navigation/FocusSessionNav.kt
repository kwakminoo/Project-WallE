package com.woli.app.navigation

/**
 * 집중 세션 종료/방향 전환 규칙.
 *
 * 가로(집중) → 세로(홈)에서 Activity가 재생성되면 Nav 백스택이 복원되어
 * 홈으로 간 직후 다시 집중 화면으로 튕길 수 있다. configChanges + forceHome으로 막는다.
 */
object FocusSessionNav {
    /** 세션 종료 화면은 HOME 위에만 쌓는다(HOME 유지). */
    const val POP_UP_TO_ON_SESSION_END = Routes.HOME
    const val POP_INCLUSIVE_ON_SESSION_END = false

    val landscapeRoutes: Set<String> = setOf(
        Routes.FOCUS_EYES,
        Routes.REMAINING_TIME,
        Routes.IMPORTANT_CALL,
        Routes.HAND_WARNING,
        Routes.FOCUS_COMPLETE,
        Routes.QUIT_CONFIRM,
        Routes.RHYTHM_MISSION,
        Routes.SESSION_REPORT,
    )

    private val completionRedirectRoutes: Set<String> = setOf(
        Routes.FOCUS_EYES,
        Routes.REMAINING_TIME,
        Routes.IMPORTANT_CALL,
        Routes.HAND_WARNING,
        Routes.QUIT_CONFIRM,
        Routes.RHYTHM_MISSION,
    )

    fun isLandscapeRoute(route: String?): Boolean =
        route != null && route in landscapeRoutes

    fun leavesFocusEyesOnBackStack(afterPopRoutes: List<String>): Boolean =
        Routes.FOCUS_EYES in afterPopRoutes

    fun shouldRedirectToNormalCompletion(route: String?): Boolean = route in completionRedirectRoutes

    /** Activity 재생성 후 복원된 라우트가 집중 세션이면 홈으로 강제해야 한다. */
    fun shouldForceHomeAfterSessionExit(forceHome: Boolean, currentRoute: String?): Boolean {
        if (!forceHome) return false
        if (currentRoute == null || currentRoute == Routes.HOME) return false
        return currentRoute in landscapeRoutes ||
            currentRoute == Routes.FOCUS_TIME ||
            currentRoute == Routes.DEVICE_CONNECT ||
            currentRoute == Routes.IMPORTANT_CONTACTS ||
            currentRoute == Routes.FOCUS_NOTIFICATION_PERMISSION ||
            currentRoute == Routes.MOUNT_GUIDE
    }
}
