package com.woli.app.focus

import kotlin.math.abs
import kotlin.random.Random

/**
 * 집중 모드 "중도 해제 방지 미션"의 리듬 게임 코어 로직.
 *
 * 기획안 4.5 — 충동적인 해제를 한 번 더 생각하게 만드는 "행동 마찰 장치".
 * 완전한 강제 차단이 아니라, 20~30초 동안 주의를 붙잡아 두는 관문이다.
 *
 * 이 파일은 순수 Kotlin(안드로이드/Compose 비의존)으로, 판정·점수·통과 규칙을
 * 단위 테스트로 검증할 수 있게 분리한다. Compose 화면은 이 엔진을 프레임마다
 * update() 하고, 사용자가 레인을 누르면 onTap() 을 호출한다.
 */

/** 노트 하나의 타격 판정 결과. */
enum class WoliRhythmJudgment {
    /** 판정선 정확히(±perfectWindow) 타격. */
    PERFECT,

    /** 살짝 빗나갔지만 인정(±goodWindow) 타격. */
    GOOD,

    /** 판정 시간을 놓쳐 흘려보낸 노트. */
    MISS,

    /** 노트가 없는 타이밍/레인을 눌러 발생한 헛침(마구 두드리기 방지). */
    STRAY,
}

/** 렌더링을 위한 노트별 상태. */
enum class WoliRhythmNoteStatus { PENDING, HIT, MISSED }

/** 차트에 배치된 노트 하나. id 는 곧 리스트 인덱스와 같다. */
data class WoliRhythmNote(
    val id: Int,
    val lane: Int,
    val hitTimeMs: Long,
) {
    /** 노트가 화면 상단에서 떨어지기 시작하는 시각. */
    fun spawnTimeMs(approachMs: Long): Long = hitTimeMs - approachMs
}

/**
 * 난이도/판정 파라미터. 프리셋은 companion 참고.
 *
 * @param requiredHits 잠금 해제에 필요한 성공(Perfect+Good) 노트 수.
 * @param maxMisfires 허용 실패치(놓친 노트 + 헛침). 초과하면 라운드 실패 → 재시도.
 */
data class WoliRhythmConfig(
    val laneCount: Int = 3,
    val bpm: Int = 95,
    val totalNotes: Int = 28,
    val leadInMs: Long = 2_500L,
    val approachMs: Long = 1_500L,
    val perfectWindowMs: Long = 80L,
    val goodWindowMs: Long = 150L,
    val requiredHits: Int = 18,
    val maxMisfires: Int = 10,
) {
    init {
        require(laneCount >= 1) { "laneCount must be >= 1" }
        require(bpm in 1..600) { "bpm out of range" }
        require(totalNotes >= 0) { "totalNotes must be >= 0" }
        require(perfectWindowMs in 1..goodWindowMs) { "windows must satisfy 0 < perfect <= good" }
        require(requiredHits in 0..totalNotes) { "requiredHits must be within [0, totalNotes]" }
    }

    /** 한 박자 길이(ms). */
    val beatIntervalMs: Long get() = 60_000L / bpm

    /** 마지막 노트가 판정 윈도우를 벗어나 라운드가 자연 종료되는 시각. */
    fun totalDurationMs(): Long {
        val lastHit = leadInMs + (totalNotes - 1).coerceAtLeast(0) * beatIntervalMs
        return lastHit + goodWindowMs + 600L
    }

    companion object {
        /** 몇 초면 통과하는 가벼운 마찰. */
        val Light = WoliRhythmConfig(
            bpm = 85, totalNotes = 16, requiredHits = 10, maxMisfires = 8,
        )

        /** 기본값: 20~30초, 집중해야 통과하는 적당한 마찰. 판정은 미션용으로 여유 있게. */
        val Moderate = WoliRhythmConfig(
            perfectWindowMs = 120L,
            goodWindowMs = 220L,
        )

        /** 발표 임팩트용: 4레인·빠른 템포·본격 게임. */
        val Intense = WoliRhythmConfig(
            laneCount = 4, bpm = 120, totalNotes = 40,
            approachMs = 1_200L, requiredHits = 30, maxMisfires = 12,
        )
    }
}

/** 노트 배치가 끝난 한 판의 악보. */
data class WoliRhythmChart(
    val config: WoliRhythmConfig,
    val notes: List<WoliRhythmNote>,
) {
    companion object {
        /**
         * 시드 기반으로 결정적(deterministic)으로 차트를 생성한다.
         * 같은 (config, seed) 는 항상 같은 노트 배치를 만든다 → 테스트 가능.
         * 같은 레인이 3연속으로 나오지 않도록 다듬어 실제로 칠 수 있게 한다.
         */
        fun generate(config: WoliRhythmConfig, seed: Long): WoliRhythmChart {
            val rng = Random(seed)
            val notes = ArrayList<WoliRhythmNote>(config.totalNotes)
            var prevLane = -1
            var prevPrevLane = -1
            for (i in 0 until config.totalNotes) {
                var lane = rng.nextInt(config.laneCount)
                var guard = 0
                while (config.laneCount > 2 && lane == prevLane && lane == prevPrevLane && guard < 8) {
                    lane = rng.nextInt(config.laneCount)
                    guard++
                }
                val hitTime = config.leadInMs + i.toLong() * config.beatIntervalMs
                notes.add(WoliRhythmNote(id = i, lane = lane, hitTimeMs = hitTime))
                prevPrevLane = prevLane
                prevLane = lane
            }
            return WoliRhythmChart(config, notes)
        }
    }
}

/** 한 번의 탭 결과. note 는 STRAY 일 때 null. deltaMs 는 (탭시각 - 판정시각), 음수면 빠름. */
data class WoliRhythmHit(
    val judgment: WoliRhythmJudgment,
    val note: WoliRhythmNote?,
    val deltaMs: Long,
)

/** 라운드 종료 시 집계. */
data class WoliRhythmResult(
    val cleared: Boolean,
    val score: Int,
    val perfect: Int,
    val good: Int,
    val miss: Int,
    val stray: Int,
    val maxCombo: Int,
    val requiredHits: Int,
) {
    /** 판정된 노트(성공+놓침) 대비 성공 비율. */
    val accuracy: Float
        get() {
            val judged = perfect + good + miss
            return if (judged == 0) 0f else (perfect + good).toFloat() / judged
        }
}

/**
 * 리듬 게임 상태 머신(로직 전용).
 *
 * 사용 흐름:
 *  1) 프레임마다 [update] 에 경과 시간(ms)을 넘겨 놓친 노트를 정리한다.
 *  2) 사용자가 레인을 누르면 [onTap] 을 호출해 판정을 받는다.
 *  3) [isFinished] 가 되면 [isCleared] 로 성공/실패를 판단하고 [result] 를 읽는다.
 *
 * 시간은 호출자가 넘기는 단조 증가 값(예: withFrameNanos 기반 경과 ms)을 그대로 쓴다.
 */
class WoliRhythmEngine(val chart: WoliRhythmChart) {
    private val config: WoliRhythmConfig = chart.config
    private val statuses: Array<WoliRhythmNoteStatus> =
        Array(chart.notes.size) { WoliRhythmNoteStatus.PENDING }

    var perfectCount: Int = 0
        private set
    var goodCount: Int = 0
        private set
    var missCount: Int = 0
        private set
    var strayCount: Int = 0
        private set
    var combo: Int = 0
        private set
    var maxCombo: Int = 0
        private set
    var score: Int = 0
        private set

    private var resolvedCount: Int = 0

    /** 성공(Perfect+Good) 노트 수. 통과 판정 기준. */
    val hitCount: Int get() = perfectCount + goodCount

    /** 실패치(놓친 노트 + 헛침). */
    val misfireCount: Int get() = missCount + strayCount

    /** 잠금 해제에 필요한 성공 노트 수를 채웠는가. */
    val isCleared: Boolean get() = hitCount >= config.requiredHits

    /** 허용 실패치를 초과했는가(재시도 필요). */
    val isFailed: Boolean get() = misfireCount > config.maxMisfires

    /** 모든 노트가 성공/놓침으로 확정되었는가. */
    val allResolved: Boolean get() = resolvedCount >= chart.notes.size

    /** 라운드가 끝났는가(성공 달성 / 실패 확정 / 노트 소진). */
    val isFinished: Boolean get() = isCleared || isFailed || allResolved

    /** 렌더링용: 인덱스 노트의 현재 상태. */
    fun statusOf(index: Int): WoliRhythmNoteStatus = statuses[index]

    /**
     * 경과 시각 기준으로 판정선을 지나쳐 버린(good 윈도우를 벗어난) 대기 노트를 놓침 처리한다.
     * 프레임마다 호출한다.
     */
    fun update(nowMs: Long) {
        for (i in chart.notes.indices) {
            if (statuses[i] != WoliRhythmNoteStatus.PENDING) continue
            val note = chart.notes[i]
            if (nowMs > note.hitTimeMs + config.goodWindowMs) {
                statuses[i] = WoliRhythmNoteStatus.MISSED
                resolvedCount++
                missCount++
                combo = 0
            }
        }
    }

    /**
     * [lane] 을 [nowMs] 시각에 눌렀을 때의 판정.
     * good 윈도우 안에 있는 같은 레인의 대기 노트 중 가장 가까운 것을 맞힌다.
     * 없으면 헛침(STRAY)으로 콤보가 끊긴다.
     */
    fun onTap(lane: Int, nowMs: Long): WoliRhythmHit {
        var bestIdx = -1
        var bestDelta = Long.MAX_VALUE
        for (i in chart.notes.indices) {
            if (statuses[i] != WoliRhythmNoteStatus.PENDING) continue
            val note = chart.notes[i]
            if (note.lane != lane) continue
            val d = abs(note.hitTimeMs - nowMs)
            if (d <= config.goodWindowMs && d < bestDelta) {
                bestDelta = d
                bestIdx = i
            }
        }

        if (bestIdx < 0) {
            strayCount++
            combo = 0
            return WoliRhythmHit(WoliRhythmJudgment.STRAY, null, 0L)
        }

        val note = chart.notes[bestIdx]
        val delta = nowMs - note.hitTimeMs
        val judgment = if (abs(delta) <= config.perfectWindowMs) {
            WoliRhythmJudgment.PERFECT
        } else {
            WoliRhythmJudgment.GOOD
        }
        statuses[bestIdx] = WoliRhythmNoteStatus.HIT
        resolvedCount++
        if (judgment == WoliRhythmJudgment.PERFECT) {
            perfectCount++
            score += 2
        } else {
            goodCount++
            score += 1
        }
        combo++
        if (combo > maxCombo) maxCombo = combo
        return WoliRhythmHit(judgment, note, delta)
    }

    fun result(): WoliRhythmResult = WoliRhythmResult(
        cleared = isCleared,
        score = score,
        perfect = perfectCount,
        good = goodCount,
        miss = missCount,
        stray = strayCount,
        maxCombo = maxCombo,
        requiredHits = config.requiredHits,
    )
}
