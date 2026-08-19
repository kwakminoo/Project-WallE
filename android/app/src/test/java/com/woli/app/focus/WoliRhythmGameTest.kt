package com.woli.app.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WoliRhythmGameTest {

    private fun engineWith(
        notes: List<WoliRhythmNote>,
        requiredHits: Int = notes.size,
        maxMisfires: Int = 100,
        perfectWindowMs: Long = 80L,
        goodWindowMs: Long = 150L,
    ): WoliRhythmEngine {
        val config = WoliRhythmConfig(
            laneCount = 3,
            totalNotes = notes.size,
            requiredHits = requiredHits,
            maxMisfires = maxMisfires,
            perfectWindowMs = perfectWindowMs,
            goodWindowMs = goodWindowMs,
        )
        return WoliRhythmEngine(WoliRhythmChart(config, notes))
    }

    private fun note(id: Int, lane: Int, hitTimeMs: Long) = WoliRhythmNote(id, lane, hitTimeMs)

    @Test
    fun chartIsDeterministicForSameSeed() {
        val a = WoliRhythmChart.generate(WoliRhythmConfig.Moderate, seed = 42L)
        val b = WoliRhythmChart.generate(WoliRhythmConfig.Moderate, seed = 42L)
        assertEquals(a.notes, b.notes)
    }

    @Test
    fun chartHasRequestedNotesWithMonotonicTimes() {
        val config = WoliRhythmConfig.Moderate
        val chart = WoliRhythmChart.generate(config, seed = 7L)
        assertEquals(config.totalNotes, chart.notes.size)
        for (i in chart.notes.indices) {
            val n = chart.notes[i]
            assertTrue("lane in range", n.lane in 0 until config.laneCount)
            if (i > 0) {
                assertTrue("times strictly increasing", n.hitTimeMs > chart.notes[i - 1].hitTimeMs)
            }
        }
    }

    @Test
    fun chartAvoidsThreeSameLaneInARow() {
        val chart = WoliRhythmChart.generate(WoliRhythmConfig.Moderate, seed = 99L)
        for (i in 2 until chart.notes.size) {
            val same = chart.notes[i].lane == chart.notes[i - 1].lane &&
                chart.notes[i].lane == chart.notes[i - 2].lane
            assertFalse("no 3 identical lanes in a row at $i", same)
        }
    }

    @Test
    fun perfectTapScoresAndBuildsCombo() {
        val engine = engineWith(listOf(note(0, 0, 1_000L), note(1, 1, 2_000L)))
        val hit = engine.onTap(lane = 0, nowMs = 1_000L)
        assertEquals(WoliRhythmJudgment.PERFECT, hit.judgment)
        assertNotNull(hit.note)
        assertEquals(1, engine.perfectCount)
        assertEquals(1, engine.hitCount)
        assertEquals(1, engine.combo)
        assertEquals(2, engine.score)
    }

    @Test
    fun slightlyOffTapCountsAsGood() {
        val engine = engineWith(listOf(note(0, 2, 1_000L)))
        // 120ms late: outside perfect(80) but inside good(150)
        val hit = engine.onTap(lane = 2, nowMs = 1_120L)
        assertEquals(WoliRhythmJudgment.GOOD, hit.judgment)
        assertEquals(1, engine.goodCount)
        assertEquals(1, engine.score)
        assertEquals(120L, hit.deltaMs)
    }

    @Test
    fun tapOutsideWindowIsStrayAndBreaksCombo() {
        val engine = engineWith(listOf(note(0, 0, 1_000L), note(1, 0, 5_000L)))
        assertEquals(WoliRhythmJudgment.PERFECT, engine.onTap(0, 1_000L).judgment)
        assertEquals(1, engine.combo)
        // 300ms late on the second note is beyond good(150) → stray
        val stray = engine.onTap(lane = 0, nowMs = 200L)
        assertEquals(WoliRhythmJudgment.STRAY, stray.judgment)
        assertNull(stray.note)
        assertEquals(0, engine.combo)
        assertEquals(1, engine.strayCount)
    }

    @Test
    fun wrongLaneTapIsStray() {
        val engine = engineWith(listOf(note(0, 0, 1_000L)))
        val hit = engine.onTap(lane = 1, nowMs = 1_000L)
        assertEquals(WoliRhythmJudgment.STRAY, hit.judgment)
        assertEquals(1, engine.strayCount)
    }

    @Test
    fun updateMarksOverdueNoteAsMiss() {
        val engine = engineWith(listOf(note(0, 1, 1_000L)))
        engine.update(nowMs = 1_000L + 150L + 1L)
        assertEquals(WoliRhythmNoteStatus.MISSED, engine.statusOf(0))
        assertEquals(1, engine.missCount)
        assertEquals(1, engine.misfireCount)
        assertTrue(engine.allResolved)
    }

    @Test
    fun tappingSameNoteTwiceMakesSecondTapStray() {
        val engine = engineWith(listOf(note(0, 0, 1_000L)))
        assertEquals(WoliRhythmJudgment.PERFECT, engine.onTap(0, 1_000L).judgment)
        assertEquals(WoliRhythmJudgment.STRAY, engine.onTap(0, 1_000L).judgment)
        assertEquals(1, engine.perfectCount)
        assertEquals(1, engine.strayCount)
    }

    @Test
    fun reachingRequiredHitsClearsRound() {
        val notes = listOf(note(0, 0, 1_000L), note(1, 1, 2_000L), note(2, 2, 3_000L))
        val engine = engineWith(notes, requiredHits = 2)
        assertFalse(engine.isCleared)
        engine.onTap(0, 1_000L)
        engine.onTap(1, 2_000L)
        assertTrue(engine.isCleared)
        assertTrue(engine.isFinished)
        assertTrue(engine.result().cleared)
    }

    @Test
    fun exceedingMaxMisfiresFailsRound() {
        val engine = engineWith(listOf(note(0, 0, 10_000L)), requiredHits = 1, maxMisfires = 3)
        repeat(4) { engine.onTap(lane = 1, nowMs = 0L) } // 4 strays > 3
        assertTrue(engine.isFailed)
        assertTrue(engine.isFinished)
        assertFalse(engine.result().cleared)
    }

    @Test
    fun resultReportsAccuracy() {
        val notes = listOf(note(0, 0, 1_000L), note(1, 1, 2_000L), note(2, 2, 3_000L), note(3, 0, 4_000L))
        val engine = engineWith(notes, requiredHits = 4)
        engine.onTap(0, 1_000L)              // perfect
        engine.onTap(1, 2_120L)              // good
        engine.update(3_000L + 150L + 1L)    // miss note 2
        // note 3 left pending
        val result = engine.result()
        assertEquals(1, result.perfect)
        assertEquals(1, result.good)
        assertEquals(1, result.miss)
        // judged = 3 (perfect+good+miss), hits = 2 → 2/3
        assertEquals(2f / 3f, result.accuracy, 0.0001f)
    }
}
