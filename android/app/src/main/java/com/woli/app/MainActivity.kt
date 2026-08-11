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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.woli.app.contacts.WoliImportantContactsStore
import com.woli.app.focus.WoliFocusSessionController
import com.woli.app.navigation.FocusSessionNav
import com.woli.app.navigation.Routes
import com.woli.app.notification.WoliNotificationAccess
import com.woli.app.ui.screens.AppInfoScreen
import com.woli.app.ui.screens.BreathingMissionScreen
import com.woli.app.ui.screens.DeviceConnectScreen
import com.woli.app.ui.screens.FocusCompleteScreen
import com.woli.app.ui.screens.FocusEyesScreen
import com.woli.app.ui.screens.FocusNotificationPermissionScreen
import com.woli.app.ui.screens.FocusTimeSettingScreen
import com.woli.app.ui.screens.HandWarningScreen
import com.woli.app.ui.screens.HardwareDiagnosticsScreen
import com.woli.app.ui.screens.HomeScreen
import com.woli.app.ui.screens.ImportantCallScreen
import com.woli.app.ui.screens.ImportantContactsScreen
import com.woli.app.ui.screens.MissionsScreen
import com.woli.app.ui.screens.MountGuideScreen
import com.woli.app.ui.screens.MemoryMissionScreen
import com.woli.app.ui.screens.NotificationDiagnosticsScreen
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
        WoliImportantContactsStore.load(applicationContext)
        WoliFocusSessionController.load(applicationContext)
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
    // 가로→세로 전환으로 Activity가 죽어도, 세션 종료 의사를 살려 집중모드 복원을 막는다.
    var forceHomeAfterSessionExit by rememberSaveable { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(startRoute) {
        val route = startRoute?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        forceHomeAfterSessionExit = false
        navController.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
            launchSingleTop = true
        }
    }

    LaunchedEffect(forceHomeAfterSessionExit, currentRoute) {
        if (FocusSessionNav.shouldForceHomeAfterSessionExit(forceHomeAfterSessionExit, currentRoute)) {
            navController.navigateHomeClearingStack()
        }
    }

    // 화면마다 SideEffect로 방향을 바꾸면 전환 중 충돌·재생성으로 집중모드가 복원된다.
    LockOrientation(activity, portrait = !FocusSessionNav.isLandscapeRoute(currentRoute))

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartFocus = {
                    forceHomeAfterSessionExit = false
                    navController.navigate(Routes.FOCUS_TIME)
                },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenMissions = { navController.navigate(Routes.MISSIONS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                onBackHome = { navController.navigateHomeClearingStack() },
                onMissions = { navController.navigate(Routes.MISSIONS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.MISSIONS) {
            MissionsScreen(
                onBackHome = { navController.navigateHomeClearingStack() },
                onStats = { navController.navigate(Routes.STATS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenBreathing = { navController.navigate(Routes.BREATHING_MISSION) },
                onOpenMemory = { navController.navigate(Routes.MEMORY_MISSION) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackHome = { navController.navigateHomeClearingStack() },
                onStats = { navController.navigate(Routes.STATS) },
                onMissions = { navController.navigate(Routes.MISSIONS) },
                onOpenGallery = { navController.navigate(Routes.SHELL_GALLERY) },
                onOpenDevice = { navController.navigate(Routes.DEVICE_CONNECT) },
                onOpenContacts = { navController.navigate(Routes.IMPORTANT_CONTACTS) },
                onOpenNotificationDiagnostics = { navController.navigate(Routes.NOTIFICATION_DIAGNOSTICS) },
                onOpenHardwareDiagnostics = { navController.navigate(Routes.HARDWARE_DIAGNOSTICS) },
                onOpenAppInfo = { navController.navigate(Routes.APP_INFO) },
            )
        }
        composable(Routes.BREATHING_MISSION) {
            LockOrientation(activity, portrait = true)
            BreathingMissionScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MEMORY_MISSION) {
            LockOrientation(activity, portrait = true)
            MemoryMissionScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.APP_INFO) {
            LockOrientation(activity, portrait = true)
            AppInfoScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.FOCUS_TIME) {
            FocusTimeSettingScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.DEVICE_CONNECT) },
            )
        }
        composable(Routes.DEVICE_CONNECT) {
            DeviceConnectScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.IMPORTANT_CONTACTS) },
            )
        }
        composable(Routes.IMPORTANT_CONTACTS) {
            ImportantContactsScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.FOCUS_NOTIFICATION_PERMISSION) },
            )
        }
        composable(Routes.FOCUS_NOTIFICATION_PERMISSION) {
            LockOrientation(activity, portrait = true)
            FocusNotificationPermissionScreen(
                onBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate(Routes.NOTIFICATION_DIAGNOSTICS) },
                onNext = { navController.navigate(Routes.MOUNT_GUIDE) },
            )
        }
        composable(Routes.NOTIFICATION_DIAGNOSTICS) {
            LockOrientation(activity, portrait = true)
            NotificationDiagnosticsScreen(
                onBack = { navController.popBackStack() },
                onOpenNotificationSettings = {
                    activity.startActivity(WoliNotificationAccess.settingsIntent())
                },
            )
        }
        composable(Routes.HARDWARE_DIAGNOSTICS) {
            LockOrientation(activity, portrait = true)
            HardwareDiagnosticsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MOUNT_GUIDE) {
            MountGuideScreen(
                onBack = { navController.popBackStack() },
                onStartFocus = { navController.navigate(Routes.FOCUS_EYES) },
            )
        }
        composable(Routes.FOCUS_EYES) {
            FocusEyesScreen(
                onShowRemaining = { navController.navigate(Routes.REMAINING_TIME) },
                onShowCall = { navController.navigate(Routes.IMPORTANT_CALL) },
                onShowWarning = { navController.navigate(Routes.HAND_WARNING) },
                onQuit = { navController.navigate(Routes.QUIT_CONFIRM) },
                onComplete = { navController.navigateSessionEnd(Routes.FOCUS_COMPLETE) },
                onOpenNotificationSettings = {
                    activity.startActivity(WoliNotificationAccess.settingsIntent())
                },
            )
        }
        composable(Routes.REMAINING_TIME) {
            RemainingTimeScreen(onBackEyes = { navController.popBackStack() })
        }
        composable(Routes.IMPORTANT_CALL) {
            ImportantCallScreen(
                onAnswer = { navController.popBackStack() },
                onLater = { navController.popBackStack() },
                onContinue = { navController.popBackStack() },
            )
        }
        composable(Routes.HAND_WARNING) {
            HandWarningScreen(onDismiss = { navController.popBackStack() })
        }
        composable(Routes.FOCUS_COMPLETE) {
            FocusCompleteScreen(
                onReport = { navController.navigateSessionEnd(Routes.SESSION_REPORT) },
                onHome = {
                    forceHomeAfterSessionExit = true
                    navController.navigateHomeClearingStack()
                },
            )
        }
        composable(Routes.QUIT_CONFIRM) {
            QuitConfirmScreen(
                onContinue = { navController.popBackStack() },
                onStartMission = { navController.navigate(Routes.RHYTHM_MISSION) },
            )
        }
        composable(Routes.RHYTHM_MISSION) {
            RhythmMissionScreen(
                onSuccess = {
                    forceHomeAfterSessionExit = true
                    navController.navigateHomeClearingStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(Routes.SESSION_REPORT) {
            SessionReportScreen(
                onHome = {
                    forceHomeAfterSessionExit = true
                    navController.navigateHomeClearingStack()
                },
            )
        }
        composable(Routes.SHELL_GALLERY) {
            ShellGalleryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { route -> navController.navigate(route) },
            )
        }
    }
}

/** 집중 세션 종료 화면: HOME까지(미포함) pop해 눈/설정 플로우로 뒤로가기 재진입을 막는다. */
private fun NavController.navigateSessionEnd(route: String) {
    navigate(route) {
        popUpTo(FocusSessionNav.POP_UP_TO_ON_SESSION_END) {
            inclusive = FocusSessionNav.POP_INCLUSIVE_ON_SESSION_END
        }
        launchSingleTop = true
    }
}

private fun NavController.navigateHomeClearingStack() {
    navigate(Routes.HOME) {
        // 그래프 루트까지 비워 재생성 시 집중 플로우가 복원되지 않게 한다.
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun LockOrientation(activity: ComponentActivity, portrait: Boolean) {
    val orientation = if (portrait) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    SideEffect {
        if (activity.requestedOrientation != orientation) {
            activity.requestedOrientation = orientation
        }
    }
}
