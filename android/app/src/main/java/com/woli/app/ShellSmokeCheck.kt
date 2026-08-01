package com.woli.app

/**
 * ponytail: 껍데기 단계 스모크 체크. UI/네비 라우트 문자열이 비어 있지 않은지만 검증.
 * 업그레이드: 실제 Compose UI 테스트 / BLE 통합 테스트로 교체.
 */
object ShellSmokeCheck {
    fun assertRoutesNonEmpty(routes: List<String>): Boolean {
        require(routes.isNotEmpty()) { "routes must not be empty" }
        require(routes.all { it.isNotBlank() }) { "blank route" }
        return true
    }
}
