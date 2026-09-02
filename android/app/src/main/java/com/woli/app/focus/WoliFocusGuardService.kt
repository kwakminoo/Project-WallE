package com.woli.app.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.woli.app.call.WoliCallAccess
import com.woli.app.call.WoliCallCenter
import com.woli.app.call.WoliCallMonitor
import com.woli.app.call.WoliCallState
import com.woli.app.device.WoliBleDeviceClient
import com.woli.app.device.WoliDeviceProtocol
import com.woli.app.notification.WoliNotificationCenter
import com.woli.app.notification.WoliNotificationPriority
import com.woli.app.voice.WoliAnnouncementFormatter
import com.woli.app.voice.WoliTtsSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class WoliFocusGuardService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ttsSpeaker: WoliTtsSpeaker? = null
    private var bleClient: WoliBleDeviceClient? = null
    private var callMonitor: WoliCallMonitor? = null
    private var guardStarted = false

    override fun onCreate() {
        super.onCreate()
        WoliFocusSessionController.load(applicationContext)
        ttsSpeaker = WoliTtsSpeaker(this)
        bleClient = WoliBleDeviceClient(this)
        callMonitor = WoliCallMonitor(applicationContext) { state, callerInfo ->
            WoliCallCenter.updateState(state, callerInfo = callerInfo)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }

        ensureNotificationChannel()
        startForegroundCompat()
        if (!guardStarted) {
            guardStarted = true
            startGuardWorkers()
        } else if (WoliFocusSessionController.current.value != null) {
            startForegroundCompat()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        guardStarted = false
        callMonitor?.stop()
        ttsSpeaker?.shutdown()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startGuardWorkers() {
        bleClient?.sendPendingFocusCommand()
        if (WoliFocusSessionController.current.value != null) {
            FocusSessionAudioMode.enterVibrate(applicationContext)
            FocusAnnouncementTracker.reset()
        }
        if (WoliCallAccess.isGranted(this)) {
            callMonitor?.start()
        }

        serviceScope.launch {
            while (true) {
                delay(1_000L)
                val nowMillis = System.currentTimeMillis()
                val active = WoliFocusSessionController.current.value ?: continue
                if (active.remainingMillis(nowMillis) <= 0L && !uiActive.get()) {
                    if (completeNormally(applicationContext, bleClient, nowMillis)) {
                        ttsSpeaker?.speak("집중 시간이 완료되어 잠금을 해제합니다.")
                        stopSelf()
                        return@launch
                    }
                }
            }
        }

        serviceScope.launch {
            while (true) {
                delay(400L)
                if (WoliFocusSessionController.current.value == null) continue
                if (FocusLockTask.isPinned(applicationContext)) continue
                if (WoliFocusGuardService.isUiActive()) continue
                if (FocusSessionRecovery.isAppInForeground(applicationContext)) continue
                FocusSessionRecovery.recover(applicationContext)
            }
        }

        serviceScope.launch {
            WoliNotificationCenter.events.collectLatest { events ->
                if (WoliFocusSessionController.current.value == null) return@collectLatest
                val event = events.firstOrNull { it.priority != WoliNotificationPriority.Normal }
                    ?: return@collectLatest
                if (!FocusAnnouncementTracker.shouldAnnounceNotification(event.id)) return@collectLatest
                if (WoliCallCenter.current.value?.state == WoliCallState.Ringing) return@collectLatest

                WoliAnnouncementFormatter.notificationAnnouncement(event)?.let { announcement ->
                    FocusAnnouncementTracker.markNotificationAnnounced(event.id)
                    ttsSpeaker?.speak(announcement, onDone = {
                        FocusVoiceInterruptState.requestListening()
                    })
                }
            }
        }

        serviceScope.launch {
            WoliCallCenter.current.collectLatest { call ->
                if (WoliFocusSessionController.current.value == null) return@collectLatest
                if (call?.state != WoliCallState.Ringing) return@collectLatest
                if (!FocusAnnouncementTracker.shouldAnnounceCall(call.id)) return@collectLatest

                WoliAnnouncementFormatter.callAnnouncement(call)?.let { announcement ->
                    FocusAnnouncementTracker.markCallAnnounced(call.id)
                    ttsSpeaker?.speak(announcement, onDone = {
                        FocusVoiceInterruptState.requestListening()
                    })
                }
            }
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "월이 집중 보호",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "집중 세션, 음성 안내, 월이 잠금 상태를 유지합니다."
        }
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundCompat() {
        val notification = FocusSessionRecovery.buildRecoveryForegroundNotification(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val ACTION_START = "com.woli.app.focus.action.START_GUARD"
        private const val ACTION_STOP = "com.woli.app.focus.action.STOP_GUARD"
        private const val CHANNEL_ID = "woli_focus_guard"
        private const val NOTIFICATION_ID = 1001
        private val uiActive = AtomicBoolean(false)

        /** Shared normal-completion path for the UI timer, voice command, and background guard. */
        fun completeNormally(
            context: Context,
            bleClient: WoliBleDeviceClient? = null,
            nowMillis: Long = System.currentTimeMillis(),
        ): Boolean {
            if (WoliFocusSessionController.completeIfActive(WoliFocusExitReason.Completed, nowMillis) == null) {
                return false
            }
            FocusSessionAudioMode.restore(context)
            FocusVoiceInterruptState.stopListening()
            FocusAnnouncementTracker.reset()
            (bleClient ?: WoliBleDeviceClient(context)).sendPendingFocusCommand()
            stop(context)
            return true
        }

        fun start(context: Context) {
            val intent = Intent(context, WoliFocusGuardService::class.java).setAction(ACTION_START)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WoliFocusGuardService::class.java).setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
        }

        fun setUiActive(active: Boolean) {
            uiActive.set(active)
        }

        fun isUiActive(): Boolean = uiActive.get()
    }
}
