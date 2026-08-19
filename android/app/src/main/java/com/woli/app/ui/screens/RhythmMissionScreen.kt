package com.woli.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.device.WoliDeviceCenter
import com.woli.app.focus.WoliFocusExitReason
import com.woli.app.focus.WoliFocusGuardService
import com.woli.app.focus.WoliFocusSessionController
import com.woli.app.focus.WoliRhythmChart
import com.woli.app.focus.WoliRhythmConfig
import com.woli.app.focus.WoliRhythmEngine
import com.woli.app.focus.WoliRhythmJudgment
import com.woli.app.focus.WoliRhythmNoteStatus
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliOrange
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliWarning
import com.woli.app.ui.theme.WoliYellow
import kotlinx.coroutines.delay

/**
 * 집중 모드 "중도 해제 방지 미션" — 리듬 게임 화면.
 *
 * 기획안 4.5의 행동 마찰 장치. 노트가 레인을 따라 떨어지고, 판정선에 닿는 순간
 * 해당 레인을 탭한다. 목표 성공 수([WoliRhythmConfig.requiredHits])를 채우면
 * 물리 잠금을 풀고([WoliDeviceCenter.setLocked]) 집중 세션을 미션 해제로 종료한다.
 * 실패치가 한도를 넘으면 라운드가 끝나고 다시 시도할 수 있다.
 *
 * 게임 규칙/판정은 [WoliRhythmEngine](순수 로직, 단위 테스트 대상)에 있고,
 * 이 화면은 프레임 루프(withFrameNanos)와 렌더링/입력만 담당한다.
 */
@Composable
fun RhythmMissionScreen(onSuccess: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val config = remember { WoliRhythmConfig.Moderate }

    // attempt 를 올리면 아래 remember 들이 재생성되어 새 판이 시작된다(다시 시도).
    var attempt by remember { mutableIntStateOf(0) }
    val chart = remember(attempt) {
        WoliRhythmChart.generate(config, seed = System.currentTimeMillis() + attempt * 1_009L)
    }
    val engine = remember(attempt) { WoliRhythmEngine(chart) }

    var nowMs by remember(attempt) { mutableLongStateOf(0L) }
    var phase by remember(attempt) { mutableStateOf(RhythmPhase.PLAYING) }
    var lastJudgment by remember(attempt) { mutableStateOf<WoliRhythmJudgment?>(null) }
    var lastJudgmentAtMs by remember(attempt) { mutableLongStateOf(-10_000L) }

    val approachMs = config.approachMs
    val firstSpawnMs = chart.notes.firstOrNull()?.spawnTimeMs(approachMs) ?: config.leadInMs
    val safetyCapMs = config.totalDurationMs() + 1_500L
    val laneColors = remember { listOf(WoliCyan, WoliYellow, WoliOrange, Color(0xFFBF5AF2)) }

    // 프레임 루프: 경과 시간(ms)을 계산해 놓친 노트를 정리하고 종료를 판단한다.
    LaunchedEffect(attempt) {
        val startNanos = withFrameNanos { it }
        while (true) {
            val frameNanos = withFrameNanos { it }
            val t = (frameNanos - startNanos) / 1_000_000L
            nowMs = t
            engine.update(t)
            if (engine.isFinished || t > safetyCapMs) {
                phase = if (engine.isCleared) RhythmPhase.CLEARED else RhythmPhase.FAILED
                break
            }
        }
    }

    // 라운드 결과 처리(성공 시 잠금 해제/세션 종료).
    LaunchedEffect(phase) {
        when (phase) {
            RhythmPhase.CLEARED -> {
                WoliFocusSessionController.recordMissionAttempt(success = true)
                WoliFocusSessionController.complete(WoliFocusExitReason.MissionUnlocked)
                WoliDeviceCenter.setLocked(false)
                WoliFocusGuardService.stop(context)
                delay(900)
                onSuccess()
            }
            RhythmPhase.FAILED -> WoliFocusSessionController.recordMissionAttempt(success = false)
            RhythmPhase.PLAYING -> Unit
        }
    }

    val onLaneTap: (Int) -> Unit = { lane ->
        if (phase == RhythmPhase.PLAYING && nowMs >= firstSpawnMs) {
            val hit = engine.onTap(lane, nowMs)
            lastJudgment = hit.judgment
            lastJudgmentAtMs = nowMs
            if (hit.judgment == WoliRhythmJudgment.PERFECT || hit.judgment == WoliRhythmJudgment.GOOD) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // 전체 화면(가로). 상단 정보 / 중앙 플레이 필드 / 하단 컨트롤.
    val t = nowMs
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── 상단 정보 ──
        Text("잠금 해제 미션 · 리듬", color = WoliMuted, fontSize = 12.sp)
        Text(
            text = "판정선에 노트가 닿는 순간, 그 레인을 탭!",
            color = WoliText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StatPill("성공", "${engine.hitCount}/${config.requiredHits}", WoliYellow)
            StatPill("콤보", "${engine.combo}", WoliCyan)
            StatPill("실패", "${engine.misfireCount}/${config.maxMisfires}", WoliWarning)
        }
        Spacer(Modifier.height(10.dp))

        // ── 중앙 플레이 필드 ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val laneCount = config.laneCount
                val laneWidth = size.width / laneCount
                val topY = size.height * 0.05f
                val judgeY = size.height * 0.80f
                val noteRadius = (laneWidth * 0.22f).coerceAtMost(size.height * 0.11f)

                // 레인 구분선
                for (l in 1 until laneCount) {
                    val x = laneWidth * l
                    drawLine(
                        color = WoliMuted.copy(alpha = 0.12f),
                        start = Offset(x, topY),
                        end = Offset(x, judgeY + noteRadius),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                // 판정선
                drawLine(
                    color = WoliCyan.copy(alpha = 0.7f),
                    start = Offset(0f, judgeY),
                    end = Offset(size.width, judgeY),
                    strokeWidth = 3.dp.toPx(),
                )

                // 판정선 위 각 레인 타깃 링
                for (l in 0 until laneCount) {
                    val cx = laneWidth * (l + 0.5f)
                    drawCircle(
                        color = laneColors[l % laneColors.size].copy(alpha = 0.45f),
                        radius = noteRadius * 1.15f,
                        center = Offset(cx, judgeY),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }

                // 떨어지는 노트
                chart.notes.forEachIndexed { i, note ->
                    if (engine.statusOf(i) != WoliRhythmNoteStatus.PENDING) return@forEachIndexed
                    val spawn = note.spawnTimeMs(approachMs)
                    if (t < spawn || t > note.hitTimeMs + config.goodWindowMs) return@forEachIndexed
                    val progress = ((t - spawn).toFloat() / approachMs).coerceIn(0f, 1.15f)
                    val cx = laneWidth * (note.lane + 0.5f)
                    val cy = topY + progress * (judgeY - topY)
                    val color = laneColors[note.lane % laneColors.size]
                    drawCircle(color.copy(alpha = 0.22f), radius = noteRadius * 1.9f, center = Offset(cx, cy))
                    drawCircle(color, radius = noteRadius, center = Offset(cx, cy))
                    drawCircle(Color.White.copy(alpha = 0.85f), radius = noteRadius * 0.34f, center = Offset(cx, cy))
                }
            }

            // 레인 탭 존(투명). 화면 폭을 레인 수로 균등 분할.
            Row(modifier = Modifier.fillMaxSize()) {
                for (lane in 0 until config.laneCount) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(attempt) {
                                detectTapGestures(onPress = { onLaneTap(lane) })
                            },
                    )
                }
            }

            // 카운트다운(시작 전)
            if (t < config.leadInMs) {
                val secs = ((config.leadInMs - t + 999L) / 1_000L).toInt().coerceAtLeast(1)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("$secs", color = WoliYellow.copy(alpha = 0.85f), fontSize = 64.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 판정 피드백(짧게 표시)
            val judgment = lastJudgment
            if (judgment != null && t - lastJudgmentAtMs in 0L..320L) {
                val (label, color) = judgmentFeedback(judgment)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(label, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 종료 오버레이
            if (phase != RhythmPhase.PLAYING) {
                RhythmResultOverlay(
                    cleared = phase == RhythmPhase.CLEARED,
                    hitCount = engine.hitCount,
                    requiredHits = config.requiredHits,
                    maxCombo = engine.maxCombo,
                    onRetry = { attempt += 1 },
                    onCancel = onCancel,
                )
            }
        }

        // ── 하단 컨트롤 ──
        Spacer(Modifier.height(10.dp))
        if (phase == RhythmPhase.PLAYING) {
            Box(modifier = Modifier.width(220.dp)) {
                WoliSecondaryButton(text = "그만두고 집중 계속", onClick = onCancel)
            }
        }
    }
}

private enum class RhythmPhase { PLAYING, CLEARED, FAILED }

private fun judgmentFeedback(judgment: WoliRhythmJudgment): Pair<String, Color> = when (judgment) {
    WoliRhythmJudgment.PERFECT -> "PERFECT" to WoliYellow
    WoliRhythmJudgment.GOOD -> "GOOD" to WoliCyan
    WoliRhythmJudgment.MISS -> "MISS" to WoliWarning
    WoliRhythmJudgment.STRAY -> "빗나감" to WoliWarning
}

@Composable
private fun StatPill(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WoliMuted, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RhythmResultOverlay(
    cleared: Boolean,
    hitCount: Int,
    requiredHits: Int,
    maxCombo: Int,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (cleared) {
                Text("성공!", color = WoliYellow, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "잠금을 해제합니다 · 최대 콤보 $maxCombo",
                    color = WoliText,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text("아쉬워요", color = WoliWarning, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "성공 $hitCount/$requiredHits · 다시 시도하거나 집중을 계속하세요",
                    color = WoliText,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                WoliPrimaryButton(text = "다시 시도", onClick = onRetry)
                Spacer(Modifier.height(10.dp))
                WoliSecondaryButton(text = "집중 계속하기", onClick = onCancel)
            }
        }
    }
}
