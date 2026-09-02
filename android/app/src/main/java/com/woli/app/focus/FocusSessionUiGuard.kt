package com.woli.app.focus

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** immersive 집중 화면에서 lock task를 Activity 수준에서 유지한다. */
@Composable
fun FocusSessionUiGuard(
    activity: ComponentActivity,
    immersiveFocus: Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(immersiveFocus, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME,
                -> if (immersiveFocus) FocusLockTask.enter(activity)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (immersiveFocus && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            FocusLockTask.enter(activity)
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            FocusLockTask.exit(activity)
        }
    }
}
