package com.woli.app.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.woli.app.MainActivity
import com.woli.app.MainActivity.Companion.EXTRA_ROUTE
import com.woli.app.R
import com.woli.app.call.WoliCallAccess
import com.woli.app.call.WoliCallCenter
import com.woli.app.call.WoliCallMonitor
import com.woli.app.call.WoliCallState
import com.woli.app.device.WoliBleDeviceClient
import com.woli.app.device.WoliDeviceCenter
import com.woli.app.device.WoliDeviceProtocol
import com.woli.app.navigation.Routes
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
    private var lastSpokenNotificationId: String? = null
    private var lastSpokenCallKey: String? = null

    override fun onCreate() {
        super.onCreate()
        ttsSpeaker = WoliTtsSpeaker(this)
        bleClient = WoliBleDeviceClient(this)
        callMonitor = WoliCallMonitor(applicationContext) { state, callerInfo ->
            WoliCallCenter.updateState(state, callerInfo = callerInfo)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startGuard()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        callMonitor?.stop()
        ttsSpeaker?.shutdown()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startGuard() {
        ensureNotificationChannel()
        startForegroundCompat()
        if (WoliCallAccess.isGranted(this)) {
            callMonitor?.start()
        }

        serviceScope.launch {
            while (true) {
                delay(1_000L)
                val nowMillis = System.currentTimeMillis()
                val active = WoliFocusSessionController.current.value
                if (active == null) {
                    stopSelf()
                    return@launch
                }
                if (active.remainingMillis(nowMillis) <= 0L) {
                    WoliFocusSessionController.complete(WoliFocusExitReason.Completed, nowMillis)
                    WoliDeviceCenter.setLocked(false)
                    bleClient?.sendCommand(WoliDeviceProtocol.COMMAND_UNLOCK)
                    ttsSpeaker?.speak("집중 시간이 완료되어 잠금을 해제합니다.")
                    stopSelf()
                    return@launch
                }
            }
        }

        serviceScope.launch {
            WoliNotificationCenter.events.collectLatest { events ->
                if (uiActive.get()) return@collectLatest
                val event = events.firstOrNull { it.priority != WoliNotificationPriority.Normal }
                    ?: return@collectLatest
                if (event.id == lastSpokenNotificationId) return@collectLatest
                if (WoliCallCenter.current.value?.state == WoliCallState.Ringing) return@collectLatest

                WoliAnnouncementFormatter.notificationAnnouncement(event)?.let { announcement ->
                    ttsSpeaker?.speak(announcement)
                    lastSpokenNotificationId = event.id
                }
            }
        }

        serviceScope.launch {
            WoliCallCenter.current.collectLatest { call ->
                if (uiActive.get()) return@collectLatest
                if (call?.state != WoliCallState.Ringing) return@collectLatest
                val key = "${call.id}_${call.state.name}"
                if (key == lastSpokenCallKey) return@collectLatest

                WoliAnnouncementFormatter.callAnnouncement(call)?.let { announcement ->
                    ttsSpeaker?.speak(announcement)
                    lastSpokenCallKey = key
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
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_woli)
            .setContentTitle("월이 집중 모드 실행 중")
            .setContentText("중요 연락과 잠금 상태를 백그라운드에서 지키고 있습니다.")
            .setContentIntent(focusPendingIntent())
            .setOngoing(true)
            .setSilent(true)
            .build()

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

    private fun focusPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(EXTRA_ROUTE, Routes.FOCUS_EYES)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        private const val ACTION_START = "com.woli.app.focus.action.START_GUARD"
        private const val ACTION_STOP = "com.woli.app.focus.action.STOP_GUARD"
        private const val CHANNEL_ID = "woli_focus_guard"
        private const val NOTIFICATION_ID = 1001
        private val uiActive = AtomicBoolean(false)

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
    }
}
