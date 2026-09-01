package com.woli.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woli.app.call.WoliCallActionController
import com.woli.app.call.WoliCallAccess
import com.woli.app.contacts.WoliContactsAccess
import com.woli.app.contacts.WoliImportantContact
import com.woli.app.contacts.WoliImportantContactSource
import com.woli.app.contacts.WoliImportantContactsStore
import com.woli.app.contacts.WoliPhoneNumberNormalizer
import com.woli.app.contacts.WoliSystemContactReader
import com.woli.app.contacts.WoliSystemContactReadResult
import com.woli.app.device.WoliBleDeviceClient
import com.woli.app.device.WoliDeviceActionResult
import com.woli.app.device.WoliDeviceCenter
import com.woli.app.device.WoliDeviceProtocol
import com.woli.app.device.WoliDeviceConnectionState
import com.woli.app.device.WoliDeviceState
import com.woli.app.device.WoliDiscoveredDevice
import com.woli.app.focus.WoliFocusSessionConfig
import com.woli.app.focus.WoliFocusSessionController
import com.woli.app.focus.FocusNotificationPermissionItem
import com.woli.app.focus.WoliFocusGuardAccess
import com.woli.app.focus.WoliFocusGuardService
import com.woli.app.focus.WoliFocusNotificationPermissions
import com.woli.app.focus.hand.CameraHandApproachAccess
import com.woli.app.notification.WoliNotificationAccess
import com.woli.app.ui.components.ShellHintBar
import com.woli.app.ui.components.WoliPrimaryButton
import com.woli.app.ui.components.WoliSecondaryButton
import com.woli.app.ui.theme.WoliBlack
import com.woli.app.ui.theme.WoliCyan
import com.woli.app.ui.theme.WoliMuted
import com.woli.app.ui.theme.WoliText
import com.woli.app.ui.theme.WoliYellow

@Composable
fun FocusTimeSettingScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val savedConfig by WoliFocusSessionController.config.collectAsState()
    var hour by remember { mutableIntStateOf(savedConfig.durationMinutes / 60) }
    var minute by remember { mutableIntStateOf(savedConfig.durationMinutes % 60) }
    var allowImportantOnly by remember { mutableStateOf(savedConfig.allowImportantOnly) }
    var breakNotify by remember { mutableStateOf(savedConfig.breakNotify) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "집중 시간 설정", onBack = onBack)
        Text(
            text = "얼마나 집중할까요?",
            color = WoliMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(20.dp))
                .padding(vertical = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeWheel(value = hour, unit = "시간", onMinus = { if (hour > 0) hour-- }, onPlus = { if (hour < 5) hour++ })
            Text(":", color = WoliYellow, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            TimeWheel(value = minute, unit = "분", onMinus = { minute = (minute + 55) % 60 }, onPlus = { minute = (minute + 5) % 60 })
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(25, 50, 90, 120).forEach { m ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(20.dp))
                        .clickable {
                            hour = m / 60
                            minute = m % 60
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("${m}분", color = WoliText, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        OptionToggle(
            title = "집중 모드",
            subtitle = "중요 연락만 허용",
            checked = allowImportantOnly,
            onCheckedChange = { allowImportantOnly = it },
        )
        Spacer(modifier = Modifier.height(10.dp))
        OptionToggle(
            title = "휴식 알림",
            subtitle = if (breakNotify) "사용 중" else "사용 안 함",
            checked = breakNotify,
            onCheckedChange = { breakNotify = it },
        )
        Spacer(modifier = Modifier.weight(1f))
        ShellHintBar(text = "설정한 시간은 집중 세션 타이머와 리포트에 반영됩니다.")
        Spacer(modifier = Modifier.height(12.dp))
        WoliPrimaryButton(
            text = "다음",
            onClick = {
                WoliFocusSessionController.updateConfig(
                    WoliFocusSessionConfig(
                        durationMinutes = (hour * 60 + minute).coerceAtLeast(5),
                        allowImportantOnly = allowImportantOnly,
                        breakNotify = breakNotify,
                    ),
                )
                onNext()
            },
        )
    }
}

@Composable
private fun TimeWheel(
    value: Int,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("+", color = WoliYellow, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onPlus))
        Text(
            text = "%02d".format(value),
            color = WoliText,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(unit, color = WoliMuted, fontSize = 13.sp)
        Text("−", color = WoliYellow, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onMinus))
    }
}

@Composable
private fun OptionToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = WoliText, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = WoliMuted, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WoliBlack,
                checkedTrackColor = WoliYellow,
            ),
        )
    }
}

@Composable
fun DeviceConnectScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val bleClient = remember(context) { WoliBleDeviceClient(context) }
    val deviceState by WoliDeviceCenter.state.collectAsState()
    var hasBlePermissions by remember {
        mutableStateOf(bleClient.hasScanPermission() && bleClient.hasConnectPermission())
    }
    var actionMessage by remember { mutableStateOf<String?>(deviceState.lastMessage) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasBlePermissions = permissions.values.all { it }
        actionMessage = if (hasBlePermissions) {
            bleClient.startScan().userMessage()
        } else {
            "Bluetooth 권한이 있어야 월이 기기를 검색할 수 있습니다."
        }
    }

    DisposableEffect(bleClient) {
        onDispose { bleClient.stopScan() }
    }

    fun requestOrScan() {
        if (bleClient.hasScanPermission() && bleClient.hasConnectPermission()) {
            hasBlePermissions = true
            actionMessage = bleClient.startScan().userMessage()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ),
            )
        }
    }

    val devices = WoliDeviceCenter.allKnownDevices()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "월이 기기 연결", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text("BLE로 월이 로봇을 연결하거나 시뮬레이션으로 시연하세요.", color = WoliMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        devices.forEach { device ->
            DeviceRow(
                device = device,
                connected = deviceState.connectedDevice?.id == device.id,
                onClick = {
                    val result = bleClient.connect(device)
                    actionMessage = result.userMessage()
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (actionMessage != null) {
            Text(
                text = actionMessage ?: "",
                color = if (deviceState.connection == WoliDeviceConnectionState.Error) WoliYellow else WoliMuted,
                fontSize = 12.sp,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        WoliSecondaryButton(
            text = if (hasBlePermissions) "다시 검색" else "Bluetooth 권한 허용",
            onClick = ::requestOrScan,
        )
        Spacer(modifier = Modifier.height(10.dp))
        WoliPrimaryButton(
            text = "다음",
            onClick = onNext,
        )
    }
}

@Composable
private fun DeviceRow(
    device: WoliDiscoveredDevice,
    connected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = WoliCyan)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, color = WoliText, fontWeight = FontWeight.SemiBold)
            Text(
                text = when {
                    connected -> "연결됨"
                    device.simulated -> "시뮬레이션"
                    device.rssi != null -> "RSSI ${device.rssi}"
                    else -> "사용 가능"
                },
                color = WoliMuted,
                fontSize = 12.sp,
            )
        }
        if (connected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF30D158))
        }
    }
}

@Composable
fun ImportantContactsScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val importantContacts by WoliImportantContactsStore.contacts.collectAsState()
    var deviceContacts by remember { mutableStateOf(emptyList<com.woli.app.contacts.WoliSystemContact>()) }
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var hasContactsPermission by remember { mutableStateOf(WoliContactsAccess.canReadContacts(context)) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasContactsPermission = granted
        actionMessage = if (granted) {
            when (val result = WoliSystemContactReader.readPhoneContacts(context)) {
                is WoliSystemContactReadResult.Success -> {
                    deviceContacts = result.contacts
                    result.userMessage()
                }
                WoliSystemContactReadResult.MissingPermission -> result.userMessage()
                is WoliSystemContactReadResult.Failed -> result.userMessage()
            }
        } else {
            "연락처 권한이 없어 수동 입력만 사용할 수 있습니다."
        }
    }

    fun refreshDeviceContacts() {
        when (val result = WoliSystemContactReader.readPhoneContacts(context)) {
            is WoliSystemContactReadResult.Success -> {
                deviceContacts = result.contacts
                actionMessage = result.userMessage()
            }
            WoliSystemContactReadResult.MissingPermission -> {
                actionMessage = result.userMessage()
            }
            is WoliSystemContactReadResult.Failed -> {
                actionMessage = result.userMessage()
            }
        }
    }

    fun addManualContact() {
        val normalizedPhone = WoliPhoneNumberNormalizer.normalize(manualPhone)
        if (manualName.isBlank() && normalizedPhone.isBlank()) {
            actionMessage = "이름 또는 전화번호를 입력하세요."
            return
        }

        WoliImportantContactsStore.upsert(
            context = context,
            contact = WoliImportantContact(
                displayName = manualName.ifBlank { WoliPhoneNumberNormalizer.mask(manualPhone) },
                phoneNumber = manualPhone,
                normalizedPhoneNumber = normalizedPhone,
                source = WoliImportantContactSource.Manual,
            ),
        )
        manualName = ""
        manualPhone = ""
        actionMessage = "중요 연락처가 저장되었습니다."
    }

    DisposableEffect(context) {
        WoliImportantContactsStore.load(context)
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
        ) {
        BackTitle(title = "중요 연락처", onBack = onBack)
        Text("집중 중 월이가 우선 안내할 연락처를 저장하세요.", color = WoliMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Text("수동 추가", color = WoliText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = manualName,
                    onValueChange = { manualName = it },
                    label = { Text("이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualPhone,
                    onValueChange = { manualPhone = it },
                    label = { Text("전화번호") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                WoliSecondaryButton(text = "중요 연락처 추가", onClick = ::addManualContact)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Text("단말 연락처", color = WoliText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (hasContactsPermission) {
                    WoliSecondaryButton(text = "연락처 불러오기", onClick = ::refreshDeviceContacts)
                    Spacer(modifier = Modifier.height(10.dp))
                    deviceContacts.take(8).forEach { contact ->
                        ContactImportRow(
                            name = contact.displayName,
                            phone = WoliPhoneNumberNormalizer.mask(contact.phoneNumber),
                            onAdd = {
                                WoliImportantContactsStore.upsert(
                                    context = context,
                                    contact = WoliImportantContact(
                                        id = "device_${contact.normalizedPhoneNumber}",
                                        displayName = contact.displayName,
                                        phoneNumber = contact.phoneNumber,
                                        normalizedPhoneNumber = contact.normalizedPhoneNumber,
                                        source = WoliImportantContactSource.Device,
                                    ),
                                )
                                actionMessage = "${contact.displayName} 연락처를 추가했습니다."
                            },
                        )
                    }
                } else {
                    Text(
                        "연락처 권한을 허용하면 단말 연락처에서 바로 추가할 수 있습니다.",
                        color = WoliMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    WoliSecondaryButton(
                        text = "연락처 권한 허용",
                        onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("저장된 중요 연락처", color = WoliText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            importantContacts.forEach { contact ->
                ImportantContactRow(
                    contact = contact,
                    onToggle = { WoliImportantContactsStore.toggleEnabled(context, contact.id) },
                    onRemove = { WoliImportantContactsStore.remove(context, contact.id) },
                )
            }
            if (importantContacts.isEmpty()) {
                Text("아직 저장된 연락처가 없습니다.", color = WoliMuted, fontSize = 13.sp)
            }
            actionMessage?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(message, color = WoliCyan, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        WoliPrimaryButton(text = "저장하고 다음", onClick = onNext)
    }
}

@Composable
private fun ImportantContactRow(
    contact: WoliImportantContact,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (contact.enabled) WoliYellow else Color(0xFF2C2C2E), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                contact.displayName.take(1).ifBlank { "?" },
                color = if (contact.enabled) WoliBlack else WoliText,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.displayName, color = WoliText)
            Text(
                text = contact.phoneNumber.ifBlank { contact.source.displayLabel() },
                color = WoliMuted,
                fontSize = 12.sp,
            )
        }
        Switch(
            checked = contact.enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = WoliBlack,
                checkedTrackColor = WoliYellow,
            ),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "삭제",
            color = WoliMuted,
            fontSize = 12.sp,
            modifier = Modifier.clickable(onClick = onRemove),
        )
    }
}

@Composable
private fun ContactImportRow(
    name: String,
    phone: String,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = WoliText, fontSize = 14.sp)
            Text(phone, color = WoliMuted, fontSize = 12.sp)
        }
        Text(
            text = "추가",
            color = WoliCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onAdd),
        )
    }
}

private fun WoliImportantContactSource.displayLabel(): String {
    return when (this) {
        WoliImportantContactSource.Default -> "기본 연락처"
        WoliImportantContactSource.Device -> "단말 연락처"
        WoliImportantContactSource.Manual -> "수동 입력"
    }
}

@Composable
fun FocusNotificationPermissionScreen(
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember { mutableStateOf(WoliFocusNotificationPermissions.read(context)) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    fun refreshAccessState() {
        permissionState = WoliFocusNotificationPermissions.read(context)
    }

    fun openSettings(intentProvider: () -> android.content.Intent, successMessage: String) {
        runCatching { context.startActivity(intentProvider()) }
            .onSuccess { actionMessage = successMessage }
            .onFailure { error ->
                actionMessage = error.message ?: "설정 화면을 열 수 없습니다. Android 설정에서 직접 권한을 확인하세요."
            }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAccessState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(permissionState.allGranted) {
        if (permissionState.allGranted) {
            onNext()
        }
    }

    val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { refreshAccessState() }
    val phoneBundlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshAccessState() }
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { refreshAccessState() }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { refreshAccessState() }

    val missingItems = permissionState.missingItems
    val optionalItems = permissionState.optionalMissingItems

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "집중 알림 전달", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "중요 알림과 전화를 월이가 전달하려면 아래 권한이 필요합니다.",
                color = WoliMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
            )
            if (missingItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(18.dp))
                        .padding(18.dp),
                ) {
                    missingItems.forEachIndexed { index, item ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        PermissionStatusRow(
                            title = item.statusTitle(),
                            value = item.statusDescription(),
                            active = false,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
            if (optionalItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(18.dp))
                        .padding(18.dp),
                ) {
                    optionalItems.forEachIndexed { index, item ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        PermissionStatusRow(
                            title = item.statusTitle(),
                            value = item.statusDescription(),
                            active = false,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
            ShellHintBar(
                text = actionMessage
                    ?: "설정 화면에서 권한을 허용한 뒤 이 화면으로 돌아오면 자동으로 확인됩니다.",
            )
            Spacer(modifier = Modifier.height(18.dp))
            missingItems.forEach { item ->
                WoliPrimaryButton(
                    text = item.buttonLabel(),
                    onClick = {
                        when (item) {
                            FocusNotificationPermissionItem.NotificationAccess -> {
                                openSettings(
                                    intentProvider = WoliNotificationAccess::settingsIntent,
                                    successMessage = "알림 접근 설정에서 월이 서비스를 허용하세요.",
                                )
                            }
                            FocusNotificationPermissionItem.PostNotifications -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            FocusNotificationPermissionItem.BatteryOptimization -> {
                                openSettings(
                                    intentProvider = {
                                        WoliFocusGuardAccess.requestBatteryOptimizationExemptionIntent(context)
                                    },
                                    successMessage = "배터리 최적화 예외 요청에서 허용을 선택하세요.",
                                )
                            }
                            FocusNotificationPermissionItem.PhoneBundle -> {
                                phoneBundlePermissionLauncher.launch(
                                    WoliFocusNotificationPermissions.phoneRuntimePermissions(),
                                )
                            }
                            FocusNotificationPermissionItem.Contacts -> {
                                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                            FocusNotificationPermissionItem.Camera -> {
                                cameraPermissionLauncher.launch(CameraHandApproachAccess.requiredPermission())
                            }
                        }
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            optionalItems.forEach { item ->
                WoliSecondaryButton(
                    text = item.buttonLabel(),
                    onClick = {
                        when (item) {
                            FocusNotificationPermissionItem.Camera -> {
                                cameraPermissionLauncher.launch(CameraHandApproachAccess.requiredPermission())
                            }
                            else -> Unit
                        }
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (missingItems.isNotEmpty()) {
                WoliSecondaryButton(
                    text = "앱별 검증 열기",
                    onClick = onOpenDiagnostics,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        WoliPrimaryButton(
            text = "권한 없이 계속",
            onClick = onNext,
        )
    }
}

private fun FocusNotificationPermissionItem.statusTitle(): String {
    return when (this) {
        FocusNotificationPermissionItem.NotificationAccess -> "알림 접근"
        FocusNotificationPermissionItem.PostNotifications -> "알림 표시"
        FocusNotificationPermissionItem.BatteryOptimization -> "배터리 제한"
        FocusNotificationPermissionItem.PhoneBundle -> "전화·발신자·제어"
        FocusNotificationPermissionItem.Contacts -> "연락처 읽기"
        FocusNotificationPermissionItem.Camera -> "카메라 (손 접근 감지)"
    }
}

private fun FocusNotificationPermissionItem.statusDescription(): String {
    return when (this) {
        FocusNotificationPermissionItem.NotificationAccess -> "알림 접근 권한 필요"
        FocusNotificationPermissionItem.PostNotifications -> "Android 13+ 알림 표시 권한 필요"
        FocusNotificationPermissionItem.BatteryOptimization -> "배터리 최적화 예외 허용 필요"
        FocusNotificationPermissionItem.PhoneBundle -> "전화 감지·발신자 식별·전화 제어 권한 필요"
        FocusNotificationPermissionItem.Contacts -> "중요 연락처 선택 권한 필요"
        FocusNotificationPermissionItem.Camera -> "집중 중 손 접근 감지용 (선택)"
    }
}

private fun FocusNotificationPermissionItem.buttonLabel(): String {
    return when (this) {
        FocusNotificationPermissionItem.NotificationAccess -> "알림 접근 설정 열기"
        FocusNotificationPermissionItem.PostNotifications -> "알림 표시 권한 허용"
        FocusNotificationPermissionItem.BatteryOptimization -> "배터리 최적화 설정 열기"
        FocusNotificationPermissionItem.PhoneBundle -> "전화·발신자·제어 권한 허용"
        FocusNotificationPermissionItem.Contacts -> "연락처 권한 허용"
        FocusNotificationPermissionItem.Camera -> "카메라 권한 허용"
    }
}

@Composable
private fun PermissionStatusRow(title: String, value: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (active) WoliYellow else WoliMuted, CircleShape),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = WoliText, fontWeight = FontWeight.SemiBold)
            Text(value, color = WoliMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun MountGuideScreen(onBack: () -> Unit, onStartFocus: () -> Unit) {
    val context = LocalContext.current
    val bleClient = remember(context) { WoliBleDeviceClient(context) }
    val deviceState by WoliDeviceCenter.state.collectAsState()
    var actionMessage by remember { mutableStateOf(deviceState.lastMessage) }
    var lockAngle by remember { mutableIntStateOf(90) }
    var unlockAngle by remember { mutableIntStateOf(10) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackTitle(title = "스마트폰 거치", onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(160.dp)
                .border(2.dp, WoliYellow, RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1E), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = WoliCyan,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "스마트폰을 가로로\n월이 머리 거치대에 올려주세요",
            color = WoliText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "거치가 확인되면 잠금이 작동하고\n집중 모드(눈 화면)로 전환됩니다.",
            color = WoliMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "거치 ${if (deviceState.isMounted) "확인" else "대기"} · 잠금 ${if (deviceState.isLocked) "작동" else "해제"}",
            color = if (deviceState.isMounted && deviceState.isLocked) WoliCyan else WoliMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Text("서보 캘리브레이션", color = WoliText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            CalibrationControlRow(
                label = "잠금 각도",
                value = lockAngle,
                onMinus = { lockAngle = (lockAngle - 5).coerceAtLeast(0) },
                onPlus = { lockAngle = (lockAngle + 5).coerceAtMost(180) },
                onSend = {
                    val result = bleClient.sendCommand(WoliDeviceProtocol.commandCalibrateLock(lockAngle))
                    actionMessage = result.userMessage()
                },
            )
            CalibrationControlRow(
                label = "해제 각도",
                value = unlockAngle,
                onMinus = { unlockAngle = (unlockAngle - 5).coerceAtLeast(0) },
                onPlus = { unlockAngle = (unlockAngle + 5).coerceAtMost(180) },
                onSend = {
                    val result = bleClient.sendCommand(WoliDeviceProtocol.commandCalibrateUnlock(unlockAngle))
                    actionMessage = result.userMessage()
                },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        ShellHintBar(text = actionMessage)
        Spacer(modifier = Modifier.height(12.dp))
        WoliSecondaryButton(
            text = "거치 확인 및 잠금",
            onClick = {
                val result = bleClient.sendCommand(WoliDeviceProtocol.COMMAND_LOCK)
                actionMessage = result.userMessage()
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        WoliPrimaryButton(
            text = "집중 모드 시작",
            onClick = {
                val alreadyActive = WoliFocusSessionController.current.value?.isActive == true
                WoliFocusSessionController.startIfNeeded(System.currentTimeMillis())
                if (!alreadyActive) {
                    bleClient.sendPendingFocusCommand()
                }
                WoliFocusGuardService.start(context)
                onStartFocus()
            },
        )
    }
}

@Composable
fun HardwareDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bleClient = remember(context) { WoliBleDeviceClient(context) }
    val deviceState by WoliDeviceCenter.state.collectAsState()
    var hasBlePermissions by remember {
        mutableStateOf(bleClient.hasScanPermission() && bleClient.hasConnectPermission())
    }
    var actionMessage by remember { mutableStateOf(deviceState.lastMessage) }
    var lockAngle by remember { mutableIntStateOf(90) }
    var unlockAngle by remember { mutableIntStateOf(10) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasBlePermissions = permissions.values.all { it }
        actionMessage = if (hasBlePermissions) {
            bleClient.startScan().userMessage()
        } else {
            "Bluetooth 권한이 있어야 실기기 진단을 실행할 수 있습니다."
        }
    }

    DisposableEffect(bleClient) {
        onDispose { bleClient.stopScan() }
    }

    fun requestOrScan() {
        if (bleClient.hasScanPermission() && bleClient.hasConnectPermission()) {
            hasBlePermissions = true
            actionMessage = bleClient.startScan().userMessage()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ),
            )
        }
    }

    fun sendCommand(label: String, command: String) {
        val result = bleClient.sendCommand(command)
        actionMessage = "$label: ${result.userMessage()}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "하드웨어 검증", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text("ESP32, 서보, 거치 센서, 손 접근 센서를 시연 전 점검합니다.", color = WoliMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            HardwareStatusCard(deviceState = deviceState)
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text("최근 BLE 기록", color = WoliText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Status: ${deviceState.lastStatusPayload ?: "아직 수신 없음"}", color = WoliMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "명령: ${deviceState.lastCommand ?: "없음"} · ${deviceState.lastCommandResult ?: "결과 없음"}",
                    color = WoliMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            WoliSecondaryButton(
                text = if (hasBlePermissions) "BLE 기기 다시 검색" else "Bluetooth 권한 허용 및 검색",
                onClick = ::requestOrScan,
            )
            Spacer(modifier = Modifier.height(10.dp))
            WoliDeviceCenter.allKnownDevices().forEach { device ->
                DeviceRow(
                device = device,
                connected = deviceState.connectedDevice?.id == device.id,
                onClick = {
                        val result = bleClient.connect(device)
                        actionMessage = result.userMessage()
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text("진단 명령", color = WoliText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                WoliSecondaryButton(
                    text = "STATUS 요청",
                    onClick = {
                        sendCommand("STATUS", WoliDeviceProtocol.COMMAND_STATUS)
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                WoliSecondaryButton(
                    text = "잠금 테스트",
                    onClick = {
                        sendCommand("LOCK", WoliDeviceProtocol.COMMAND_LOCK)
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                WoliSecondaryButton(
                    text = "해제 테스트",
                    onClick = {
                        sendCommand("UNLOCK", WoliDeviceProtocol.COMMAND_UNLOCK)
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                WoliSecondaryButton(
                    text = "집중 시작 테스트",
                    onClick = {
                        sendCommand("START", WoliDeviceProtocol.COMMAND_START)
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                WoliSecondaryButton(
                    text = "정상 종료 테스트",
                    onClick = {
                        sendCommand("SESSION_END", WoliDeviceProtocol.COMMAND_SESSION_END)
                    },
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text("서보 캘리브레이션", color = WoliText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                CalibrationControlRow(
                    label = "잠금 각도",
                    value = lockAngle,
                    onMinus = { lockAngle = (lockAngle - 5).coerceAtLeast(0) },
                    onPlus = { lockAngle = (lockAngle + 5).coerceAtMost(180) },
                    onSend = {
                        sendCommand("CAL_LOCK=$lockAngle", WoliDeviceProtocol.commandCalibrateLock(lockAngle))
                    },
                )
                CalibrationControlRow(
                    label = "해제 각도",
                    value = unlockAngle,
                    onMinus = { unlockAngle = (unlockAngle - 5).coerceAtLeast(0) },
                    onPlus = { unlockAngle = (unlockAngle + 5).coerceAtMost(180) },
                    onSend = {
                        sendCommand("CAL_UNLOCK=$unlockAngle", WoliDeviceProtocol.commandCalibrateUnlock(unlockAngle))
                    },
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            if (deviceState.connectedDevice?.simulated == true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Text("센서 시뮬레이션", color = WoliText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    WoliSecondaryButton(
                        text = if (deviceState.isMounted) "거치 해제 상태로 전환" else "거치 감지 상태로 전환",
                        onClick = {
                            WoliDeviceCenter.setMounted(!deviceState.isMounted)
                            actionMessage = "거치 센서 시뮬레이션을 변경했습니다."
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WoliSecondaryButton(
                        text = if (deviceState.isHandNear) "손 접근 해제 상태로 전환" else "손 접근 감지 상태로 전환",
                        onClick = {
                            WoliDeviceCenter.setHandNear(!deviceState.isHandNear)
                            actionMessage = "손 접근 센서 시뮬레이션을 변경했습니다."
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            ShellHintBar(text = actionMessage)
        }
    }
}

@Composable
private fun HardwareStatusCard(deviceState: WoliDeviceState) {
    val device = deviceState.connectedDevice
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text("현재 상태", color = WoliText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        PermissionStatusRow(
            title = "BLE 연결",
            value = deviceState.connection.displayLabel(),
            active = deviceState.isConnected,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            title = "기기명",
            value = device?.name ?: "미연결",
            active = device != null,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            title = "주소",
            value = device?.id ?: "미수신",
            active = device != null,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            title = "RSSI",
            value = device?.rssi?.let { "$it dBm" } ?: "미수신",
            active = device?.rssi != null,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            title = "거치 센서",
            value = if (deviceState.isMounted) "스마트폰 거치됨" else "거치 대기",
            active = deviceState.isMounted,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            title = "잠금 서보",
            value = if (deviceState.isLocked) "잠금 위치" else "해제 위치",
            active = deviceState.isLocked,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            title = "집중 세션",
            value = if (deviceState.isSessionActive) "진행 중" else "대기",
            active = deviceState.isSessionActive,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            title = "손 접근 센서",
            value = if (deviceState.isHandNear) "접근 감지" else "정상",
            active = !deviceState.isHandNear,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            title = "배터리",
            value = deviceState.hardwareStatus.batteryPercent?.let { "$it%" } ?: "미수신",
            active = deviceState.hardwareStatus.batteryPercent != null,
        )
    }
}

private fun WoliDeviceConnectionState.displayLabel(): String {
    return when (this) {
        WoliDeviceConnectionState.Disconnected -> "연결 안 됨"
        WoliDeviceConnectionState.Scanning -> "검색 중"
        WoliDeviceConnectionState.Connecting -> "연결 중"
        WoliDeviceConnectionState.DiscoveringServices -> "서비스 준비 중"
        WoliDeviceConnectionState.Ready -> "실기기 연결됨"
        WoliDeviceConnectionState.Simulated -> "시뮬레이션 연결됨"
        WoliDeviceConnectionState.Error -> "오류"
    }
}

@Composable
private fun CalibrationControlRow(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = WoliMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("−", color = WoliYellow, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onMinus))
        Text("${value}°", color = WoliText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("+", color = WoliYellow, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onPlus))
        Text(
            "전송",
            color = WoliCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.clickable(onClick = onSend),
        )
    }
}

@Composable
fun BackTitle(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "뒤로",
            tint = WoliText,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 12.dp),
        )
        Text(text = title, color = WoliText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ShellGalleryScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val items = listOf(
        "home" to "홈",
        "focus_time" to "집중 시간 설정",
        "device_connect" to "기기 연결",
        "important_contacts" to "중요 연락처",
        "focus_notification_permission" to "집중 알림 전달",
        "notification_diagnostics" to "앱별 알림 검증",
        "hardware_diagnostics" to "하드웨어 검증",
        "mount_guide" to "거치 안내",
        "focus_eyes" to "집중 눈 화면",
        "remaining_time" to "남은 시간 표시",
        "important_call" to "중요 연락",
        "hand_warning" to "손 접근 경고",
        "focus_complete" to "집중 완료",
        "quit_confirm" to "중도 해제 확인",
        "rhythm_mission" to "리듬 미션",
        "session_report" to "세션 리포트",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoliBlack)
            .padding(20.dp),
    ) {
        BackTitle(title = "화면 상태 갤러리", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text("SW 예상 시나리오 화면을 개별 확인합니다.", color = WoliMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            items.forEach { (route, label) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                        .clickable { onOpen(route) }
                        .padding(16.dp),
                ) {
                    Text(label, color = WoliText)
                }
            }
        }
    }
}
