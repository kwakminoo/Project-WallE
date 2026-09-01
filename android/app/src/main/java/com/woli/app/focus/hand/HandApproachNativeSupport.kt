package com.woli.app.focus.hand

/** Probes MediaPipe JNI once; x86_64 emulators ship no matching .so. */
object HandApproachNativeSupport {
    private val lock = Any()

    @Volatile
    private var probed = false

    @Volatile
    private var available = false

    fun isAvailable(): Boolean {
        if (probed) return available
        synchronized(lock) {
            if (probed) return available
            available = runCatching {
                System.loadLibrary("mediapipe_tasks_vision_jni")
                true
            }.getOrDefault(false)
            probed = true
            return available
        }
    }
}
