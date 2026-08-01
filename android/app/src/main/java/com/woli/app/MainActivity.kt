package com.woli.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woli.app.navigation.Routes
import com.woli.app.ui.screens.DeviceConnectScreen
import com.woli.app.ui.screens.FocusCompleteScreen
import com.woli.app.ui.screens.FocusEyesScreen
import com.woli.app.ui.screens.FocusTimeSettingScreen
import com.woli.app.ui.screens.HandWarningScreen
import com.woli.app.ui.screens.HomeScreen
import com.woli.app.ui.screens.ImportantCallScreen
import com.woli.app.ui.screens.ImportantContactsScreen
import com.woli.app.ui.screens.MissionsScreen
import com.woli.app.ui.screens.MountGuideScreen
import com.woli.app.ui.screens.QuitConfirmScreen
import com.woli.app.ui.screens.RemainingTimeScreen
import com.woli.app.ui.screens.RhythmMissionScreen
import com.woli.app.ui.screens.SessionReportScreen
import com.woli.app.ui.screens.SettingsScreen
import com.woli.app.ui.screens.ShellGalleryScreen
import com.woli.app.ui.screens.StatsScreen
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliTheme

class MainActivity : ComponentActivity() {
    private val routeState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        routeState.value = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            WoliTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = WoliBlack) {
                    WoliApp(activity = this, startRoute = routeState.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeState.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}

@Composable
fun WoliApp(activity: ComponentActivity, startRoute: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(startRoute) {
        val route = startRoute?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        navController.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            LockOrientation(activity, portrait = true)
            HomeScreen(
                onStartFocus = { navController.navigate(Routes.FOCUS_TIME) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenMissions = { navController.navigate(Routes.MISSIONS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.STATS) {
            LockOrientation(activity, portrait = true)
            StatsScreen(
                onBackHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onMissions = { navController.navigate(Routes.MISSIONS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.MISSIONS) {
            LockOrientation(activity, portrait = true)
            MissionsScreen(
                onBackHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onStats = { navController.navigate(Routes.STATS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            LockOrientation(activity, portrait = true)
            SettingsScreen(
                onBackHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onStats = { navController.navigate(Routes.STATS) },
                onMissions = { navController.navigate(Routes.MISSIONS) },
                onOpenGallery = { navController.navigate(Routes.SHELL_GALLERY) },
                onOpenDevice = { navController.navigate(Routes.DEVICE_CONNECT) },
                onOpenContacts = { navController.navigate(Routes.IMPORTANT_CONTACTS) },
            )
        }
        composable(Routes.FOCUS_TIME) {
            LockOrientation(activity, portrait = true)
            FocusTimeSettingScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.DEVICE_CONNECT) },
            )
        }
        composable(Routes.DEVICE_CONNECT) {
            LockOrientation(activity, portrait = true)
            DeviceConnectScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.IMPORTANT_CONTACTS) },
            )
        }
        composable(Routes.IMPORTANT_CONTACTS) {
            LockOrientation(activity, portrait = true)
            ImportantContactsScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.MOUNT_GUIDE) },
            )
        }
        composable(Routes.MOUNT_GUIDE) {
            LockOrientation(activity, portrait = true)
            MountGuideScreen(
                onBack = { navController.popBackStack() },
                onStartFocus = { navController.navigate(Routes.FOCUS_EYES) },
            )
        }
        composable(Routes.FOCUS_EYES) {
            LockOrientation(activity, portrait = false)
            FocusEyesScreen(
                onShowRemaining = { navController.navigate(Routes.REMAINING_TIME) },
                onShowCall = { navController.navigate(Routes.IMPORTANT_CALL) },
                onShowWarning = { navController.navigate(Routes.HAND_WARNING) },
                onQuit = { navController.navigate(Routes.QUIT_CONFIRM) },
                onComplete = { navController.navigate(Routes.FOCUS_COMPLETE) },
            )
        }
        composable(Routes.REMAINING_TIME) {
            LockOrientation(activity, portrait = false)
            RemainingTimeScreen(onBackEyes = { navController.popBackStack() })
        }
        composable(Routes.IMPORTANT_CALL) {
            LockOrientation(activity, portrait = false)
            ImportantCallScreen(
                onAnswer = { navController.popBackStack() },
                onLater = { navController.popBackStack() },
                onContinue = { navController.popBackStack() },
            )
        }
        composable(Routes.HAND_WARNING) {
            LockOrientation(activity, portrait = false)
            HandWarningScreen(onDismiss = { navController.popBackStack() })
        }
        composable(Routes.FOCUS_COMPLETE) {
            LockOrientation(activity, portrait = false)
            FocusCompleteScreen(
                onReport = { navController.navigate(Routes.SESSION_REPORT) },
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.QUIT_CONFIRM) {
            LockOrientation(activity, portrait = false)
            QuitConfirmScreen(
                onContinue = { navController.popBackStack() },
                onStartMission = { navController.navigate(Routes.RHYTHM_MISSION) },
            )
        }
        composable(Routes.RHYTHM_MISSION) {
            LockOrientation(activity, portrait = false)
            RhythmMissionScreen(
                onSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(Routes.SESSION_REPORT) {
            LockOrientation(activity, portrait = false)
            SessionReportScreen(
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SHELL_GALLERY) {
            LockOrientation(activity, portrait = true)
            ShellGalleryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { route -> navController.navigate(route) },
            )
        }
    }
}

@Composable
private fun LockOrientation(activity: ComponentActivity, portrait: Boolean) {
    DisposableEffect(portrait) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation = if (portrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        onDispose {
            activity.requestedOrientation = previous
        }
    }
}
