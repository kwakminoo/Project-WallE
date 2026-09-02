package com.woli.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** 집중 세션 중 상태/내비게이션 바를 숨기고, 스와이프로만 잠깐 노출되게 한다. */
@Composable
fun FocusSystemUiEffect(activity: ComponentActivity, immersive: Boolean) {
    val window = activity.window
    val view = LocalView.current

    DisposableEffect(immersive, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        if (immersive) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
        onDispose {
            val resetController = WindowCompat.getInsetsController(window, view)
            resetController.show(WindowInsetsCompat.Type.systemBars())
            resetController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    SideEffect {
        if (immersive) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
