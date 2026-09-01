package com.woli.app.focus.hand

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandApproachEvaluatorTest {
    private val config = HandApproachEvalConfig(
        nearAreaThreshold = 0.18f,
        exitAreaThreshold = 0.08f,
        enterFramesRequired = 4,
        enterWindowSize = 6,
        exitFramesRequired = 8,
        exitWindowSize = 10,
    )

    private val farFrame = HandApproachFrame(
        handDetected = true,
        handAreaRatio = 0.05f,
        overlapsCenter = true,
    )

    private val nearFrame = HandApproachFrame(
        handDetected = true,
        handAreaRatio = 0.25f,
        overlapsCenter = true,
    )

    @Test
    fun farHandIsNotNear() {
        val evaluator = HandApproachEvaluator(config)
        repeat(10) {
            assertFalse(evaluator.evaluate(farFrame))
        }
    }

    @Test
    fun centerLargeHandWithDebounceEntersNear() {
        val evaluator = HandApproachEvaluator(config)
        repeat(3) {
            assertFalse(evaluator.evaluate(nearFrame))
        }
        assertTrue(evaluator.evaluate(nearFrame))
        assertTrue(evaluator.evaluate(nearFrame))
    }

    @Test
    fun nearMaintainedWithoutExtraStateFlip() {
        val evaluator = HandApproachEvaluator(config)
        repeat(4) { evaluator.evaluate(nearFrame) }
        assertTrue(evaluator.evaluate(nearFrame))
        repeat(5) {
            assertTrue(evaluator.evaluate(nearFrame))
        }
    }

    @Test
    fun nearReleasesAfterExitDebounce() {
        val evaluator = HandApproachEvaluator(config)
        repeat(4) { evaluator.evaluate(nearFrame) }
        assertTrue(evaluator.evaluate(nearFrame))

        repeat(7) {
            assertTrue(evaluator.evaluate(farFrame))
        }
        assertFalse(evaluator.evaluate(farFrame))
    }

    @Test
    fun resetClearsNearState() {
        val evaluator = HandApproachEvaluator(config)
        repeat(4) { evaluator.evaluate(nearFrame) }
        assertTrue(evaluator.evaluate(nearFrame))

        evaluator.reset()
        assertFalse(evaluator.evaluate(nearFrame))
    }
}
