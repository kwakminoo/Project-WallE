package com.woli.app.focus.hand

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow

/** App-wide camera hand-proximity source; separate from BLE `hand` state. */
object WoliCameraHandApproachCenter {
    private val detector = CameraHandApproachDetector()

    val isHandNear: StateFlow<Boolean> = detector.isHandNear

    fun isDetectionSupported(): Boolean = HandApproachNativeSupport.isAvailable()

    fun startDetection(context: Context, lifecycleOwner: LifecycleOwner) {
        if (!CameraHandApproachAccess.isGranted(context) || !isDetectionSupported()) return
        detector.start(context, lifecycleOwner)
    }

    fun stopDetection() {
        detector.stop()
    }
}
