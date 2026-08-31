package com.woli.app.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WoliDiscoveredDevice(
    val id: String,
    val name: String,
    val rssi: Int? = null,
    val simulated: Boolean = false,
)

data class WoliDeviceState(
    val connection: WoliDeviceConnectionState = WoliDeviceConnectionState.Disconnected,
    val devices: List<WoliDiscoveredDevice> = emptyList(),
    val connectedDevice: WoliDiscoveredDevice? = null,
    val hardwareStatus: WoliDeviceHardwareStatus = WoliDeviceHardwareStatus(),
    val lastStatusPayload: String? = null,
    val lastCommand: String? = null,
    val lastCommandResult: String? = null,
    val lastMessage: String = "월이 기기를 검색하세요.",
) {
    val isConnected: Boolean
        get() = connection == WoliDeviceConnectionState.Ready || connection == WoliDeviceConnectionState.Simulated

    val isLocked: Boolean
        get() = hardwareStatus.locked

    val isMounted: Boolean
        get() = hardwareStatus.mounted

    val isHandNear: Boolean
        get() = hardwareStatus.handNear

    val isSessionActive: Boolean
        get() = hardwareStatus.sessionActive
}

enum class WoliDeviceConnectionState {
    Disconnected,
    Scanning,
    Connecting,
    DiscoveringServices,
    Ready,
    Simulated,
    Error,
}

object WoliDeviceCenter {
    private val simulatedDevice = WoliDiscoveredDevice(
        id = "simulated-woli",
        name = "WOLI-Simulator",
        simulated = true,
    )

    private val _state = MutableStateFlow(WoliDeviceState())
    val state: StateFlow<WoliDeviceState> = _state.asStateFlow()

    fun markScanning() {
        _state.update {
            it.copy(
                connection = WoliDeviceConnectionState.Scanning,
                lastMessage = "BLE 기기를 검색하는 중입니다.",
            )
        }
    }

    fun markScanStopped() {
        _state.update {
            if (it.connection != WoliDeviceConnectionState.Scanning) it else {
                it.copy(
                    connection = WoliDeviceConnectionState.Disconnected,
                    lastMessage = "BLE 검색을 중지했습니다.",
                )
            }
        }
    }

    fun upsertDevice(device: WoliDiscoveredDevice) {
        _state.update { current ->
            current.copy(
                devices = (listOf(device) + current.devices.filterNot { it.id == device.id })
                    .sortedWith(compareByDescending<WoliDiscoveredDevice> { !it.simulated }.thenBy { it.name }),
                lastMessage = "${device.name} 발견",
            )
        }
    }

    fun connectSimulated() {
        val status = WoliDeviceHardwareStatus(
            mounted = true,
            locked = false,
            handNear = false,
            batteryPercent = 100,
            sessionActive = false,
        )
        _state.value = WoliDeviceState(
            connection = WoliDeviceConnectionState.Simulated,
            devices = listOf(simulatedDevice),
            connectedDevice = simulatedDevice,
            hardwareStatus = status,
            lastStatusPayload = WoliDeviceProtocol.encodeStatus(status),
            lastMessage = "시뮬레이션 기기로 연결되었습니다.",
        )
    }

    fun markConnecting(device: WoliDiscoveredDevice) {
        _state.update {
            it.copy(
                connection = WoliDeviceConnectionState.Connecting,
                connectedDevice = device,
                lastMessage = "${device.name}에 연결하는 중입니다.",
            )
        }
    }

    fun markDiscoveringServices(device: WoliDiscoveredDevice) {
        _state.update {
            it.copy(
                connection = WoliDeviceConnectionState.DiscoveringServices,
                connectedDevice = device,
                lastMessage = "${device.name} 서비스 정보를 확인하는 중입니다.",
            )
        }
    }

    fun markReady(device: WoliDiscoveredDevice? = _state.value.connectedDevice) {
        _state.update {
            it.copy(
                connection = WoliDeviceConnectionState.Ready,
                connectedDevice = device ?: it.connectedDevice,
                lastMessage = "${(device ?: it.connectedDevice)?.name ?: "월이"} 연결 준비 완료",
            )
        }
    }

    fun disconnect(message: String = "기기 연결이 해제되었습니다.") {
        _state.update {
            it.copy(
                connection = WoliDeviceConnectionState.Disconnected,
                connectedDevice = null,
                lastMessage = "$message 마지막 수신 상태를 유지합니다.",
            )
        }
    }

    fun markError(message: String) {
        _state.update {
            it.copy(
                connection = WoliDeviceConnectionState.Error,
                lastMessage = message,
            )
        }
    }

    fun applyStatus(status: WoliDeviceHardwareStatus, rawPayload: String? = null) {
        _state.update {
            it.copy(
                hardwareStatus = status,
                lastStatusPayload = rawPayload ?: WoliDeviceProtocol.encodeStatus(status),
                lastMessage = "mount=${status.mounted}, lock=${status.locked}, hand=${status.handNear}, session=${status.sessionActive}",
            )
        }
    }

    fun recordCommand(command: String, result: WoliDeviceActionResult) {
        _state.update {
            it.copy(
                lastCommand = command,
                lastCommandResult = result.userMessage(),
                lastMessage = "$command: ${result.userMessage()}",
            )
        }
    }

    fun setMounted(mounted: Boolean) {
        updateSimulatedStatus { it.copy(mounted = mounted) }
    }

    fun setLocked(locked: Boolean) {
        updateSimulatedStatus { it.copy(locked = locked) }
    }

    fun setHandNear(handNear: Boolean) {
        updateSimulatedStatus { it.copy(handNear = handNear) }
    }

    private fun updateSimulatedStatus(transform: (WoliDeviceHardwareStatus) -> WoliDeviceHardwareStatus) {
        _state.update {
            if (it.connection == WoliDeviceConnectionState.Simulated) {
                val status = transform(it.hardwareStatus)
                it.copy(
                    hardwareStatus = status,
                    lastStatusPayload = WoliDeviceProtocol.encodeStatus(status),
                )
            } else {
                it
            }
        }
    }

    fun allKnownDevices(): List<WoliDiscoveredDevice> {
        val devices = _state.value.devices
        return if (devices.any { it.simulated }) devices else devices + simulatedDevice
    }
}

sealed class WoliDeviceActionResult {
    data object Started : WoliDeviceActionResult()
    data object Queued : WoliDeviceActionResult()
    data object Sent : WoliDeviceActionResult()
    data object MissingPermission : WoliDeviceActionResult()
    data object BluetoothUnavailable : WoliDeviceActionResult()
    data object NotConnected : WoliDeviceActionResult()
    data object NotReady : WoliDeviceActionResult()
    data object Busy : WoliDeviceActionResult()
    data class Failed(val reason: String) : WoliDeviceActionResult()

    val isSuccess: Boolean
        get() = this is Started || this is Queued || this is Sent

    fun userMessage(): String {
        return when (this) {
            Started -> "요청을 시작했습니다."
            Queued -> "이전 BLE 명령 뒤에 대기 중입니다."
            Sent -> "월이에게 명령을 보냈습니다."
            MissingPermission -> "Bluetooth 권한이 필요합니다."
            BluetoothUnavailable -> "이 기기에서 Bluetooth를 사용할 수 없습니다."
            NotConnected -> "월이 기기가 연결되어 있지 않습니다."
            NotReady -> "월이 BLE 서비스를 준비하는 중입니다."
            Busy -> "이전 BLE 명령을 처리하는 중입니다."
            is Failed -> reason
        }
    }
}
