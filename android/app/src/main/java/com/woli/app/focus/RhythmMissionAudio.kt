package com.woli.app.focus

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.sin

/** 리듬 미션 배경 비트·타격 효과음. 외부 에셋 없이 짧은 PCM 톤으로 재생한다. */
object RhythmMissionAudio {
    private const val SAMPLE_RATE = 44100

    private val minBufferBytes = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(2)

    private val playbackHandler = Handler(Looper.getMainLooper())

    // C5 → E5 → G5 → E5 루프
    private val MELODY_HZ = doubleArrayOf(523.25, 659.25, 783.99, 659.25)

    private var hitTone: ToneGenerator? = null

    private val audioAttributes = AudioAttributes.Builder()
        // ponytail: SONIFICATION은 에뮬레이터에서 SYSTEM/알림 스트림(음소거)으로 갈 수 있다 → MUSIC.
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    fun playBeat(beatIndex: Int) {
        playTone(196.0, 90, 0.45f)
        playTone(MELODY_HZ[beatIndex % MELODY_HZ.size], 130, 0.4f)
    }

    fun playHit(judgment: WoliRhythmJudgment) {
        when (judgment) {
            WoliRhythmJudgment.PERFECT,
            WoliRhythmJudgment.GOOD,
            -> {
                runCatching {
                    val tone = hitTone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 100).also { hitTone = it }
                    when (judgment) {
                        WoliRhythmJudgment.PERFECT -> tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
                        WoliRhythmJudgment.GOOD -> tone.startTone(ToneGenerator.TONE_PROP_ACK, 65)
                        else -> Unit
                    }
                }
            }
            WoliRhythmJudgment.MISS,
            WoliRhythmJudgment.STRAY,
            -> Unit
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Float) {
        val toneSamples = SAMPLE_RATE * durationMs / 1000
        if (toneSamples <= 0) return

        val allocSamples = maxOf(toneSamples, minBufferBytes / 2)
        val allocBytes = allocSamples * 2

        val buffer = ShortArray(allocSamples)
        val attack = (toneSamples / 10).coerceAtLeast(1)
        val release = (toneSamples / 5).coerceAtLeast(1)
        for (i in 0 until toneSamples) {
            val env = when {
                i < attack -> i.toFloat() / attack
                i > toneSamples - release -> (toneSamples - i).toFloat() / release
                else -> 1f
            }
            val sample = sin(2 * PI * freqHz * i / SAMPLE_RATE) * Short.MAX_VALUE * volume * env
            buffer[i] = sample.toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(allocBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return
        }

        track.write(buffer, 0, buffer.size)
        track.setVolume(volume)
        track.play()
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    t?.release()
                }

                override fun onPeriodicNotification(t: AudioTrack?) = Unit
            },
            playbackHandler,
        )
        track.notificationMarkerPosition = toneSamples - 1
    }
}
