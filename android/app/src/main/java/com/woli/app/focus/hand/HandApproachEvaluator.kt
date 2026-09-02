package com.woli.app.focus.hand

/** Single-frame hand proximity signals from landmark detection (normalized 0..1 coords). */
data class HandApproachFrame(
    val handDetected: Boolean,
    val handAreaRatio: Float,
    val overlapsCenter: Boolean,
) {
    companion object {
        // ponytail: center band is 40% width × 50% height; tune constants in HandApproachEvalConfig.
        private const val CENTER_MIN_X = 0.20f
        private const val CENTER_MAX_X = 0.80f
        private const val CENTER_MIN_Y = 0.15f
        private const val CENTER_MAX_Y = 0.85f

        fun fromBoundingBox(
            handDetected: Boolean,
            minX: Float,
            minY: Float,
            maxX: Float,
            maxY: Float,
        ): HandApproachFrame {
            if (!handDetected) {
                return HandApproachFrame(handDetected = false, handAreaRatio = 0f, overlapsCenter = false)
            }
            val width = (maxX - minX).coerceAtLeast(0f)
            val height = (maxY - minY).coerceAtLeast(0f)
            val areaRatio = (width * height).coerceIn(0f, 1f)
            val overlaps = minX < CENTER_MAX_X &&
                maxX > CENTER_MIN_X &&
                minY < CENTER_MAX_Y &&
                maxY > CENTER_MIN_Y
            return HandApproachFrame(
                handDetected = true,
                handAreaRatio = areaRatio,
                overlapsCenter = overlaps,
            )
        }
    }
}

/** Tunable thresholds for hand-near hysteresis and debounce windows. */
data class HandApproachEvalConfig(
    // ponytail: Jump2 640p 전면 분석 기준; 센서 전환 전 마지막 카메라 튜닝.
    val nearAreaThreshold: Float = 0.035f,
    val exitAreaThreshold: Float = 0.022f,
    val enterFramesRequired: Int = 2,
    val enterWindowSize: Int = 3,
    val exitFramesRequired: Int = 5,
    val exitWindowSize: Int = 7,
)

/** Pure debounced near/far state machine for camera hand approach. */
class HandApproachEvaluator(
    private val config: HandApproachEvalConfig = HandApproachEvalConfig(),
) {
    private var isNear = false
    private val enterWindow = ArrayDeque<Boolean>(config.enterWindowSize)
    private val exitWindow = ArrayDeque<Boolean>(config.exitWindowSize)

    fun evaluate(frame: HandApproachFrame): Boolean {
        val meetsEnter = frame.handDetected &&
            frame.handAreaRatio >= config.nearAreaThreshold &&
            frame.overlapsCenter
        val meetsExit = !frame.handDetected ||
            frame.handAreaRatio < config.exitAreaThreshold ||
            !frame.overlapsCenter

        if (!isNear) {
            push(enterWindow, config.enterWindowSize, meetsEnter)
            if (enterWindow.count { it } >= config.enterFramesRequired) {
                isNear = true
                enterWindow.clear()
            }
        } else {
            push(exitWindow, config.exitWindowSize, meetsExit)
            if (exitWindow.count { it } >= config.exitFramesRequired) {
                isNear = false
                exitWindow.clear()
            }
        }
        return isNear
    }

    fun reset() {
        isNear = false
        enterWindow.clear()
        exitWindow.clear()
    }

    private fun push(window: ArrayDeque<Boolean>, maxSize: Int, value: Boolean) {
        if (window.size >= maxSize) window.removeFirst()
        window.addLast(value)
    }
}
