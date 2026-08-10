package com.woli.app

/**
 * Lightweight route smoke check. Full UI, BLE, and telephony behavior is covered
 * by focused unit tests plus Android device validation.
 */
object ShellSmokeCheck {
    fun assertRoutesNonEmpty(routes: List<String>): Boolean {
        require(routes.isNotEmpty()) { "routes must not be empty" }
        require(routes.all { it.isNotBlank() }) { "blank route" }
        return true
    }
}
