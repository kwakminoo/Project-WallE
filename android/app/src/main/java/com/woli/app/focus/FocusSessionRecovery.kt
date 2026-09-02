package com.woli.app.focus

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.woli.app.MainActivity
import com.woli.app.R
import com.woli.app.navigation.Routes

/** 집중 세션 이탈 시 UI를 앞으로 되돌린다. FGS·Activity 양쪽에서 호출. */
object FocusSessionRecovery {
    const val COOLDOWN_MS = 350L
    private const val RECOVERY_CHANNEL_ID = "woli_focus_recovery"
    private const val RECOVERY_NOTIFICATION_ID = 1002
    private const val RECOVERY_REQUEST_CODE = 7102

    @Volatile
    var lastRecoveryAtMs: Long = 0L

    fun shouldAttemptRecovery(nowMillis: Long, lastAtMs: Long = lastRecoveryAtMs): Boolean =
        nowMillis - lastAtMs >= COOLDOWN_MS

    fun recover(
        context: Context,
        route: String = Routes.FOCUS_EYES,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (FocusLockTask.isPinned(context)) return false
        if (!shouldAttemptRecovery(nowMillis)) return false
        lastRecoveryAtMs = nowMillis

        val activity = context as? Activity
        if (activity != null) {
            FocusLockTask.enter(activity)
            if (FocusLockTask.isPinned(context)) return true
            moveTaskToFront(activity)
        }

        val pendingIntent = recoveryPendingIntent(context, route)
        // API 34+ 백그라운드 Activity 시작은 BAL에 막히므로 full-screen 알림만 사용한다.
        postRecoveryFullScreenNotification(context, pendingIntent)
        return true
    }

    fun recoveryPendingIntent(context: Context, route: String = Routes.HAND_WARNING): PendingIntent {
        return PendingIntent.getActivity(
            context,
            RECOVERY_REQUEST_CODE,
            recoveryIntent(context, route),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** FGS 알림에 full-screen intent를 붙여 잠금·알림 탭 시 복귀 경로를 제공한다. */
    fun buildRecoveryForegroundNotification(context: Context): android.app.Notification {
        val pendingIntent = recoveryPendingIntent(context)
        ensureRecoveryChannel(context)
        return NotificationCompat.Builder(context, RECOVERY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_woli)
            .setContentTitle("월이 집중 모드 실행 중")
            .setContentText("집중 화면으로 돌아갑니다.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    internal fun recoveryIntent(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, route)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )

    fun isAppInForeground(context: Context): Boolean {
        val processName = context.packageName
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
        return activityManager.runningAppProcesses.orEmpty().any { process ->
            process.processName == processName &&
                process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    private fun moveTaskToFront(activity: Activity) {
        runCatching {
            activity.getSystemService(ActivityManager::class.java)
                ?.moveTaskToFront(activity.taskId, ActivityManager.MOVE_TASK_WITH_HOME)
        }
        runCatching {
            val activityManager = activity.getSystemService(ActivityManager::class.java) ?: return
            activityManager.appTasks.forEach { task ->
                if (task.taskInfo.baseActivity?.packageName == activity.packageName) {
                    task.moveToFront()
                }
            }
        }
    }

    private fun ensureRecoveryChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            RECOVERY_CHANNEL_ID,
            "집중 복귀",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "집중 모드 이탈 시 앱으로 되돌립니다."
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun postRecoveryFullScreenNotification(context: Context, fullScreenIntent: PendingIntent) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureRecoveryChannel(context)
        val notification = NotificationCompat.Builder(context, RECOVERY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_woli)
            .setContentTitle("집중 모드")
            .setContentText("집중 화면으로 돌아갑니다.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(RECOVERY_NOTIFICATION_ID, notification)
    }
}
