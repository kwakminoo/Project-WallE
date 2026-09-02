package com.woli.app.focus

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build

/** 화면 고정(lock task)으로 홈/최근앱 이탈을 줄인다. */
object FocusLockTask {
    fun enter(activity: Activity) {
        runCatching {
            if (!isPinned(activity)) {
                activity.startLockTask()
            }
        }
    }

    fun exit(activity: Activity) {
        runCatching {
            if (isPinned(activity)) {
                activity.stopLockTask()
            }
        }
    }

    fun isPinned(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
        return activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }
}
