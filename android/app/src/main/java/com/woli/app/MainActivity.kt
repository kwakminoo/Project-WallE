package com.woli.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.woli.app.focus.FocusCallNavigation
import com.woli.app.focus.FocusLockTask
import com.woli.app.focus.FocusSessionAudioMode
import com.woli.app.focus.FocusSessionRecovery
import com.woli.app.focus.FocusSessionUiGuard
import com.woli.app.focus.FocusEscapeNavigation
import com.woli.app.focus.FocusHandApproachMonitor
import com.woli.app.focus.FocusHandApproachNavigation
import com.woli.app.focus.FocusVoiceInterruptHandler
import com.woli.app.focus.focusHandApproachMonitorEnabled
import com.woli.app.focus.returnToFocusEyes
import com.woli.app.focus.WoliFocusNotificationPermissions
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.woli.app.contacts.WoliImportantContactsStore
import com.woli.app.device.WoliDeviceCenter
import com.woli.app.focus.WoliFocusGuardService
import com.woli.app.focus.WoliFocusSessionController
import com.woli.app.focus.WoliFocusExitReason
import com.woli.app.navigation.FocusSessionNav
import com.woli.app.navigation.Routes
import com.woli.app.notification.WoliNotificationAccess
import com.woli.app.notification.WoliNotificationRuleStore
import com.woli.app.ui.FocusSystemUiEffect
import com.woli.app.ui.screens.AppInfoScreen
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
import com.woli.app.ui.screens.MountGuideScreen
import com.woli.app.ui.screens.NotificationDiagnosticsScreen
import com.woli.app.ui.screens.NotificationPolicyScreen
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
        WoliImportantContactsStore.load(applicationContext)
        WoliFocusSessionController.load(applicationContext)
        WoliNotificationRuleStore.load(applicationContext)
        routeState.value = intent?.getStringExtra(EXTRA_ROUTE)
        if (isDebuggable() && intent?.getBooleanExtra(EXTRA_TEST_AUTO_FOCUS, false) == true) {
            WoliFocusSessionController.startIfNeeded()
            WoliFocusGuardService.start(applicationContext)
            if (routeState.value.isNullOrBlank()) {
                routeState.value = Routes.FOCUS_EYES
            }
        }
        setContent {
            WoliTheme {
                WoliApp(activity = this, startRoute = routeState.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeState.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    override fun onResume() {
        super.onResume()
        if (WoliFocusSessionController.current.value != null) {
            WoliFocusGuardService.setUiActive(true)
            FocusLockTask.enter(this)
        }
    }

    override fun onPause() {
        if (WoliFocusSessionController.current.value != null) {
            WoliFocusGuardService.setUiActive(false)
        }
        super.onPause()
    }

    override fun onUserLeaveHint() {
        if (WoliFocusSessionController.current.value == null) {
            super.onUserLeaveHint()
            return
        }
        FocusLockTask.enter(this)
        if (!FocusLockTask.isPinned(this)) {
            FocusSessionRecovery.recover(this)
        }
    }

    override fun onStop() {
        if (WoliFocusSessionController.current.value != null) {
            WoliFocusGuardService.setUiActive(false)
        }
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus || WoliFocusSessionController.current.value == null) return
        if (FocusLockTask.isPinned(this)) return
        if (!FocusSessionRecovery.isAppInForeground(applicationContext)) {
            FocusSessionRecovery.recover(applicationContext)
        }
    }

    private fun isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    companion object {
        const val EXTRA_ROUTE = "route"
        /** Debug/emulator: 집중 세션·FGS를 바로 켜고 focus_eyes로 진입한다. */
        const val EXTRA_TEST_AUTO_FOCUS = "test_auto_focus"
    }
}

@Composable
fun WoliApp(activity: ComponentActivity, startRoute: String? = null) {
    val navController = rememberNavController()
    // 가로→세로 전환으로 Activity가 죽어도, 세션 종료 의사를 살려 집중모드 복원을 막는다.
    var forceHomeAfterSessionExit by rememberSaveable { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentFocusSession by WoliFocusSessionController.current.collectAsState()
    val deviceState by WoliDeviceCenter.state.collectAsState()
    val focusHistory by WoliFocusSessionController.history.collectAsState()
    val latestCompletedSession = focusHistory.firstOrNull()
    val immersiveFocus = currentFocusSession != null &&
        currentRoute in FocusSessionNav.immersiveFocusRoutes

    FocusSystemUiEffect(activity = activity, immersive = immersiveFocus)
    FocusSessionUiGuard(activity = activity, immersiveFocus = immersiveFocus)
    SideEffect {
        if (immersiveFocus) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val surfaceModifier = if (immersiveFocus) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    }

    if (currentFocusSession != null &&
        currentRoute in FocusSessionNav.immersiveFocusRoutes &&
        currentRoute != Routes.QUIT_CONFIRM &&
        currentRoute != Routes.RHYTHM_MISSION
    ) {
        BackHandler {
            navController.navigate(Routes.QUIT_CONFIRM) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(currentFocusSession?.id) {
        val context = activity.applicationContext
        if (currentFocusSession != null) {
            FocusSessionAudioMode.enterVibrate(context)
        } else {
            FocusSessionAudioMode.restore(context)
        }
    }

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

    LaunchedEffect(currentFocusSession?.id, latestCompletedSession?.id, currentRoute) {
        if (currentFocusSession == null &&
            latestCompletedSession?.exitReason == WoliFocusExitReason.Completed &&
            FocusSessionNav.shouldRedirectToNormalCompletion(currentRoute)
        ) {
            navController.navigateSessionEnd(Routes.FOCUS_COMPLETE)
        }
    }

    // 화면마다 SideEffect로 방향을 바꾸면 전환 중 충돌·재생성으로 집중모드가 복원된다.
    LockOrientation(activity, portrait = !FocusSessionNav.isLandscapeRoute(currentRoute))

    FocusHandApproachNavigation(
        navController = navController,
        currentRoute = currentRoute,
        currentFocusSession = currentFocusSession,
    )

    FocusEscapeNavigation(
        navController = navController,
        currentRoute = currentRoute,
        currentFocusSession = currentFocusSession,
    )

    FocusCallNavigation(
        navController = navController,
        currentRoute = currentRoute,
        currentFocusSession = currentFocusSession,
    )

    FocusVoiceInterruptHandler(
        navController = navController,
        currentRoute = currentRoute,
        currentFocusSession = currentFocusSession,
    )

    FocusHandApproachMonitor(
        enabled = focusHandApproachMonitorEnabled(currentRoute, currentFocusSession),
    )

    Surface(modifier = surfaceModifier, color = WoliBlack) {
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
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPermissionSetup = { navController.navigate(Routes.FOCUS_NOTIFICATION_PERMISSION) },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                onBackHome = { navController.navigateHomeClearingStack() },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackHome = { navController.navigateHomeClearingStack() },
                onStats = { navController.navigate(Routes.STATS) },
                onOpenPermissionSetup = { navController.navigate(Routes.FOCUS_NOTIFICATION_PERMISSION) },
                onOpenGallery = { navController.navigate(Routes.SHELL_GALLERY) },
                onOpenBluetoothSettings = { navController.navigate(Routes.BLUETOOTH_SETTINGS) },
                onOpenContacts = { navController.navigate(Routes.IMPORTANT_CONTACTS) },
                onOpenNotificationPolicy = { navController.navigate(Routes.NOTIFICATION_POLICY) },
                onOpenNotificationDiagnostics = { navController.navigate(Routes.NOTIFICATION_DIAGNOSTICS) },
                onOpenHardwareDiagnostics = { navController.navigate(Routes.HARDWARE_DIAGNOSTICS) },
                onOpenAppInfo = { navController.navigate(Routes.APP_INFO) },
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
                onNext = {
                    if (deviceState.isConnected) {
                        navController.navigate(Routes.IMPORTANT_CONTACTS)
                    } else {
                        navController.navigate(Routes.DEVICE_CONNECT)
                    }
                },
            )
        }
        composable(Routes.DEVICE_CONNECT) {
            DeviceConnectScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.IMPORTANT_CONTACTS) },
            )
        }
        composable(Routes.BLUETOOTH_SETTINGS) {
            LockOrientation(activity, portrait = true)
            DeviceConnectScreen(
                onBack = { navController.popBackStack() },
                onNext = null,
            )
        }
        composable(Routes.IMPORTANT_CONTACTS) {
            val context = LocalContext.current
            ImportantContactsScreen(
                onBack = { navController.popBackStack() },
                onNext = {
                    if (WoliFocusNotificationPermissions.allGranted(context)) {
                        navController.navigate(Routes.MOUNT_GUIDE)
                    } else {
                        navController.navigate(Routes.FOCUS_NOTIFICATION_PERMISSION)
                    }
                },
            )
        }
        composable(Routes.FOCUS_NOTIFICATION_PERMISSION) {
            LockOrientation(activity, portrait = true)
            FocusNotificationPermissionScreen(
                onBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate(Routes.NOTIFICATION_DIAGNOSTICS) },
                onNext = {
                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                    if (previousRoute == Routes.IMPORTANT_CONTACTS) {
                        navController.navigate(Routes.MOUNT_GUIDE) {
                            popUpTo(Routes.FOCUS_NOTIFICATION_PERMISSION) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable(Routes.NOTIFICATION_POLICY) {
            LockOrientation(activity, portrait = true)
            NotificationPolicyScreen(
                onBack = { navController.popBackStack() },
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
                onQuit = { navController.navigate(Routes.QUIT_CONFIRM) },
                onComplete = { navController.navigateSessionEnd(Routes.FOCUS_COMPLETE) },
            )
        }
        composable(Routes.REMAINING_TIME) {
            RemainingTimeScreen(onBackEyes = { navController.popBackStack() })
        }
        composable(Routes.IMPORTANT_CALL) {
            ImportantCallScreen(
                onAnswer = { navController.returnToFocusEyes() },
                onLater = { navController.returnToFocusEyes() },
                onContinue = { navController.returnToFocusEyes() },
            )
        }
        composable(Routes.HAND_WARNING) {
            HandWarningScreen(onDismiss = { navController.returnToFocusEyes() })
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
