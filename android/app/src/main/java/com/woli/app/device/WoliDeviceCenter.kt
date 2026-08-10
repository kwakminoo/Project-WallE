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
    val lastMessage: String = "월이 기기를 검색하세요.",
) {
    val isConnected: Boolean
        get() = connection == WoliDeviceConnectionState.Connected || connection == WoliDeviceConnectionState.Simulated

    val isLocked: Boolean
        get() = hardwareStatus.locked

    val isMounted: Boolean
        get() = hardwareStatus.mounted

    val isHandNear: Boolean
        get() = hardwareStatus.handNear
}

enum class WoliDeviceConnectionState {
    Disconnected,
    Scanning,
    Connecting,
    Connected,
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
        _state.value = WoliDeviceState(
            connection = WoliDeviceConnectionState.Simulated,
            devices = listOf(simulatedDevice),
            connectedDevice = simulatedDevice,
            hardwareStatus = WoliDeviceHardwareStatus(
                mounted = true,
                locked = true,
                handNear = false,
                batteryPercent = 100,
            ),
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

    fun markConnected(device: WoliDiscoveredDevice) {
        _state.update {
            it.copy(
                connection = WoliDeviceConnectionState.Connected,
                connectedDevice = device,
                lastMessage = "${device.name} 연결 완료",
            )
        }
    }

    fun disconnect(message: String = "기기 연결이 해제되었습니다.") {
        _state.update {
            it.copy(
                connection = WoliDeviceConnectionState.Disconnected,
                connectedDevice = null,
                hardwareStatus = WoliDeviceHardwareStatus(),
                lastMessage = message,
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

    fun applyStatus(status: WoliDeviceHardwareStatus) {
        _state.update {
            it.copy(
                hardwareStatus = status,
                lastMessage = "mount=${status.mounted}, lock=${status.locked}, hand=${status.handNear}",
            )
        }
    }

    fun setMounted(mounted: Boolean) {
        _state.update {
            it.copy(hardwareStatus = it.hardwareStatus.copy(mounted = mounted))
        }
    }

    fun setLocked(locked: Boolean) {
        _state.update {
            it.copy(hardwareStatus = it.hardwareStatus.copy(locked = locked))
        }
    }

    fun setHandNear(handNear: Boolean) {
        _state.update {
            it.copy(hardwareStatus = it.hardwareStatus.copy(handNear = handNear))
        }
    }

    fun allKnownDevices(): List<WoliDiscoveredDevice> {
        val devices = _state.value.devices
        return if (devices.any { it.simulated }) devices else devices + simulatedDevice
    }
}

sealed class WoliDeviceActionResult {
    data object Started : WoliDeviceActionResult()
    data object Sent : WoliDeviceActionResult()
    data object MissingPermission : WoliDeviceActionResult()
    data object BluetoothUnavailable : WoliDeviceActionResult()
    data object NotConnected : WoliDeviceActionResult()
    data class Failed(val reason: String) : WoliDeviceActionResult()

    val isSuccess: Boolean
        get() = this is Started || this is Sent

    fun userMessage(): String {
        return when (this) {
            Started -> "요청을 시작했습니다."
            Sent -> "월이에게 명령을 보냈습니다."
            MissingPermission -> "Bluetooth 권한이 필요합니다."
            BluetoothUnavailable -> "이 기기에서 Bluetooth를 사용할 수 없습니다."
            NotConnected -> "월이 기기가 연결되어 있지 않습니다."
            is Failed -> reason
        }
    }
}
