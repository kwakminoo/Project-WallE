package com.woli.app.focus



import android.content.Context

import android.media.AudioManager

import android.media.ToneGenerator

import android.os.Build

import android.os.VibrationEffect

import android.os.Vibrator

import androidx.compose.animation.core.Animatable

import androidx.compose.animation.core.AnimationVector1D

import androidx.compose.animation.core.tween



object WoliFocusEscapeFeedback {

    fun playSfx(context: Context) {

        runCatching {

            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)

            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 90)

            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60)

            tone.release()

        }

        val vibrator = context.getSystemService(Vibrator::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            vibrator.vibrate(VibrationEffect.createOneShot(90L, VibrationEffect.DEFAULT_AMPLITUDE))

        } else {

            @Suppress("DEPRECATION")

            vibrator.vibrate(90L)

        }

    }

}



suspend fun Animatable<Float, AnimationVector1D>.runHandWarningShake() {

    repeat(5) {

        animateTo(11f, tween(35))

        animateTo(-11f, tween(35))

    }

    animateTo(0f, tween(40))

}


