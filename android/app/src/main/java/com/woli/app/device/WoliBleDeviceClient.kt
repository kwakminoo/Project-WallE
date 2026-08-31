@file:Suppress("DEPRECATION")

package com.woli.app.device

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.woli.app.focus.WoliFocusSessionController
import java.nio.charset.StandardCharsets

class WoliBleDeviceClient(private val context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter = bluetoothManager?.adapter
    private val loggedDeviceIds = mutableSetOf<String>()
    private var lastLoggedStatusPayload: String? = null

    companion object {
        private const val TAG = "WOLI/BLE"
        private val operationLock = Any()

        private var gatt: BluetoothGatt? = null
        private var commandCharacteristic: BluetoothGattCharacteristic? = null
        private var statusCharacteristic: BluetoothGattCharacteristic? = null
        private var connectedDevice: WoliDiscoveredDevice? = null
        private var pendingCommandGatt: BluetoothGatt? = null
        private var pendingCommand: String? = null
        private var queuedCommand: String? = null
        private var initialStatusReadGatt: BluetoothGatt? = null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "scan failed: $errorCode")
            WoliDeviceCenter.markError("BLE 검색 실패: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(callbackGatt: BluetoothGatt, status: Int, newState: Int) {
            if (!isCurrentGatt(callbackGatt)) {
                closeStaleGatt(callbackGatt)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        failGatt(callbackGatt, "BLE 연결 상태 오류: $status")
                        return
                    }
                    val device = currentConnectedDevice()
                    if (device == null) {
                        failGatt(callbackGatt, "연결한 월이 기기 정보를 찾을 수 없습니다.")
                        return
                    }
                    Log.d(TAG, "client connected: ${device.name} (${device.id})")
                    WoliDeviceCenter.markDiscoveringServices(device)
                    if (!hasConnectPermission() || !callbackGatt.discoverServices()) {
                        failGatt(callbackGatt, "월이 BLE 서비스 검색을 시작하지 못했습니다.")
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "client disconnected: status=$status")
                    closeGatt(callbackGatt)
                    WoliDeviceCenter.disconnect("월이 BLE 연결이 해제되었습니다.")
                }

                else -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        failGatt(callbackGatt, "BLE 연결 상태 오류: $status")
                    } else {
                        Log.d(TAG, "connection state changed: $newState")
                    }
                }
            }
        }

        override fun onServicesDiscovered(callbackGatt: BluetoothGatt, status: Int) {
            if (!isCurrentGatt(callbackGatt)) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failGatt(callbackGatt, "월이 BLE 서비스 검색 실패: $status")
                return
            }

            val service = callbackGatt.getService(WoliDeviceProtocol.SERVICE_UUID)
                ?: return failGatt(callbackGatt, "WOLI GATT Service를 찾을 수 없습니다.")
            val command = service.getCharacteristic(WoliDeviceProtocol.COMMAND_UUID)
                ?: return failGatt(callbackGatt, "WOLI Command Characteristic을 찾을 수 없습니다.")
            val statusChar = service.getCharacteristic(WoliDeviceProtocol.STATUS_UUID)
                ?: return failGatt(callbackGatt, "WOLI Status Characteristic을 찾을 수 없습니다.")

            if (!command.supportsWrite()) {
                return failGatt(callbackGatt, "WOLI Command Characteristic이 WRITE를 지원하지 않습니다.")
            }
            if (!statusChar.supportsReadNotify()) {
                return failGatt(callbackGatt, "WOLI Status Characteristic은 READ와 NOTIFY를 모두 지원해야 합니다.")
            }
            val descriptor = statusChar.getDescriptor(WoliDeviceProtocol.CLIENT_CONFIG_UUID)
                ?: return failGatt(callbackGatt, "WOLI Status CCCD를 찾을 수 없습니다.")
            if (!hasConnectPermission() || !callbackGatt.setCharacteristicNotification(statusChar, true)) {
                return failGatt(callbackGatt, "WOLI Status Notify를 활성화하지 못했습니다.")
            }

            synchronized(operationLock) {
                commandCharacteristic = command
                statusCharacteristic = statusChar
            }
            if (!writeDescriptor(callbackGatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                failGatt(callbackGatt, "WOLI Status CCCD 쓰기를 시작하지 못했습니다.")
            }
        }

        override fun onDescriptorWrite(
            callbackGatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (!isCurrentGatt(callbackGatt) || descriptor.uuid != WoliDeviceProtocol.CLIENT_CONFIG_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failGatt(callbackGatt, "WOLI Status CCCD 설정 실패: $status")
                return
            }

            Log.d(TAG, "status notify ready; reading initial status")
            requestInitialStatusRead(callbackGatt)
        }

        override fun onCharacteristicChanged(
            callbackGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (isCurrentGatt(callbackGatt) && characteristic.uuid == WoliDeviceProtocol.STATUS_UUID) {
                handleStatusPayload(value)
            }
        }

        @Deprecated("Deprecated Android callback retained for API compatibility")
        override fun onCharacteristicChanged(
            callbackGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (isCurrentGatt(callbackGatt) && characteristic.uuid == WoliDeviceProtocol.STATUS_UUID) {
                handleStatusPayload(characteristic.value ?: return)
            }
        }

        override fun onCharacteristicRead(
            callbackGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleStatusRead(callbackGatt, characteristic, value, status)
        }

        @Deprecated("Deprecated Android callback retained for API compatibility")
        override fun onCharacteristicRead(
            callbackGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            handleStatusRead(callbackGatt, characteristic, characteristic.value, status)
        }

        override fun onCharacteristicWrite(
            callbackGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (!isCurrentGatt(callbackGatt) || characteristic.uuid != WoliDeviceProtocol.COMMAND_UUID) return
            val command = synchronized(operationLock) {
                pendingCommand.takeIf { pendingCommandGatt === callbackGatt }.also {
                    pendingCommand = null
                    pendingCommandGatt = null
                }
            } ?: return

            val result = if (status == BluetoothGatt.GATT_SUCCESS) {
                WoliDeviceActionResult.Sent
            } else {
                WoliDeviceActionResult.Failed("$command 쓰기 실패: $status")
            }
            WoliDeviceCenter.recordCommand(command, result)
            Log.d(TAG, "command callback: $command, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                WoliFocusSessionController.markHardwareCommandDelivered(command)
            }
            if (status == BluetoothGatt.GATT_SUCCESS ||
                WoliFocusSessionController.pendingHardwareCommand.value != command
            ) {
                dispatchPendingFocusCommand()
            }
            dispatchQueuedCommand()
        }
    }

    fun hasScanPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasConnectPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startScan(): WoliDeviceActionResult {
        val result = when {
            !hasScanPermission() -> WoliDeviceActionResult.MissingPermission
            runCatching { bluetoothAdapter?.isEnabled == false }.getOrDefault(false) -> WoliDeviceActionResult.BluetoothUnavailable
            else -> {
                val scanner = bluetoothAdapter?.bluetoothLeScanner
                    ?: return WoliDeviceActionResult.BluetoothUnavailable
                WoliDeviceCenter.markScanning()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                runCatching {
                    scanner.startScan(null, settings, scanCallback)
                    WoliDeviceActionResult.Started
                }.getOrElse { error ->
                    WoliDeviceCenter.markError(error.message ?: "BLE 검색을 시작하지 못했습니다.")
                    WoliDeviceActionResult.Failed(error.message ?: "BLE 검색을 시작하지 못했습니다.")
                }
            }
        }
        Log.d(TAG, "scan requested: ${result.userMessage()}")
        return result
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (hasScanPermission()) {
            runCatching { bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        }
        WoliDeviceCenter.markScanStopped()
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WoliDiscoveredDevice): WoliDeviceActionResult {
        if (device.simulated) {
            stopScan()
            disconnect()
            WoliDeviceCenter.connectSimulated()
            sendPendingFocusCommand()
            return WoliDeviceActionResult.Started
        }
        if (!hasConnectPermission()) return WoliDeviceActionResult.MissingPermission
        val adapter = bluetoothAdapter ?: return WoliDeviceActionResult.BluetoothUnavailable

        stopScan()
        closeGatt()
        WoliDeviceCenter.markConnecting(device)
        synchronized(operationLock) {
            connectedDevice = device
        }

        val result = runCatching {
            val remoteDevice = adapter.getRemoteDevice(device.id)
            val newGatt = remoteDevice.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                ?: error("월이 BLE 연결을 만들지 못했습니다.")
            synchronized(operationLock) {
                gatt = newGatt
                commandCharacteristic = null
                statusCharacteristic = null
                pendingCommand = null
                pendingCommandGatt = null
                queuedCommand = null
                initialStatusReadGatt = null
            }
            WoliDeviceActionResult.Started
        }.getOrElse { error ->
            WoliDeviceCenter.markError(error.message ?: "월이 BLE 연결에 실패했습니다.")
            WoliDeviceActionResult.Failed(error.message ?: "월이 BLE 연결에 실패했습니다.")
        }
        Log.d(TAG, "connect ${device.name}: ${result.userMessage()}")
        return result
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val activeGatt = currentGatt()
        if (hasConnectPermission()) runCatching { activeGatt?.disconnect() }
        closeGatt(activeGatt)
        WoliDeviceCenter.disconnect()
        Log.d(TAG, "disconnect requested")
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String): WoliDeviceActionResult {
        val result = when {
            WoliDeviceCenter.state.value.connection == WoliDeviceConnectionState.Simulated -> {
                applySimulatedCommand(command)
                WoliDeviceActionResult.Sent
            }

            !hasConnectPermission() -> WoliDeviceActionResult.MissingPermission
            WoliDeviceCenter.state.value.connection != WoliDeviceConnectionState.Ready -> {
                if (currentGatt() == null) WoliDeviceActionResult.NotConnected else WoliDeviceActionResult.NotReady
            }

            else -> writeCommand(command)
        }
        WoliDeviceCenter.recordCommand(command, result)
        Log.d(TAG, "command $command: ${result.userMessage()}")
        return result
    }

    /** Delivers the session controller's persisted START/STOP/SESSION_END intent when possible. */
    fun sendPendingFocusCommand(): WoliDeviceActionResult? {
        val command = WoliFocusSessionController.pendingHardwareCommand.value ?: return null
        if (isCommandScheduled(command)) return WoliDeviceActionResult.Queued
        val result = sendCommand(command)
        if (WoliDeviceCenter.state.value.connection == WoliDeviceConnectionState.Simulated && result.isSuccess) {
            WoliFocusSessionController.markHardwareCommandDelivered(command)
        }
        return result
    }

    @SuppressLint("MissingPermission")
    private fun writeCommand(command: String): WoliDeviceActionResult {
        val activeGatt = currentGatt() ?: return WoliDeviceActionResult.NotConnected
        val characteristic = currentCommandCharacteristic() ?: return WoliDeviceActionResult.NotReady
        if (isInitialStatusReadPending(activeGatt)) return WoliDeviceActionResult.Busy

        synchronized(operationLock) {
            if (pendingCommand != null) {
                if (queuedCommand == null) {
                    queuedCommand = command
                    return WoliDeviceActionResult.Queued
                }
                return WoliDeviceActionResult.Busy
            }
            pendingCommand = command
            pendingCommandGatt = activeGatt
        }

        val payload = command.toByteArray(StandardCharsets.UTF_8)
        val accepted = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activeGatt.writeCharacteristic(
                    characteristic,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                characteristic.value = payload
                activeGatt.writeCharacteristic(characteristic)
            }
        }.getOrElse { error ->
            clearPendingCommand(activeGatt, command)
            Log.w(TAG, "command write exception: $command", error)
            return WoliDeviceActionResult.Failed(error.message ?: "명령 전송에 실패했습니다.")
        }

        if (!accepted) {
            clearPendingCommand(activeGatt, command)
            return WoliDeviceActionResult.Failed("$command 명령을 BLE에 등록하지 못했습니다.")
        }
        return WoliDeviceActionResult.Sent
    }

    @SuppressLint("MissingPermission")
    private fun requestInitialStatusRead(callbackGatt: BluetoothGatt) {
        val characteristic = currentStatusCharacteristic()
            ?: return failGatt(callbackGatt, "초기 WOLI Status Characteristic을 찾을 수 없습니다.")
        synchronized(operationLock) {
            initialStatusReadGatt = callbackGatt
        }
        if (!callbackGatt.readCharacteristic(characteristic)) {
            clearInitialStatusRead(callbackGatt)
            WoliDeviceCenter.markReady(currentConnectedDevice())
            sendCommand(WoliDeviceProtocol.COMMAND_STATUS)
            dispatchPendingFocusCommand()
        }
    }

    private fun handleStatusRead(
        callbackGatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray?,
        status: Int,
    ) {
        if (!isCurrentGatt(callbackGatt) || characteristic.uuid != WoliDeviceProtocol.STATUS_UUID) return
        val initialRead = clearInitialStatusRead(callbackGatt)
        if (status == BluetoothGatt.GATT_SUCCESS && value != null) {
            handleStatusPayload(value)
        } else {
            Log.w(TAG, "status read failed: $status")
        }
        if (initialRead) {
            WoliDeviceCenter.markReady(currentConnectedDevice())
            if (status != BluetoothGatt.GATT_SUCCESS || value == null) {
                sendCommand(WoliDeviceProtocol.COMMAND_STATUS)
            }
            dispatchPendingFocusCommand()
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val name = if (hasConnectPermission()) {
            runCatching { device.name }.getOrNull()
        } else {
            null
        } ?: result.scanRecord?.deviceName ?: return

        if (!name.startsWith(WoliDeviceProtocol.DEVICE_NAME_PREFIX, ignoreCase = true)) return

        val discovered = WoliDiscoveredDevice(
            id = device.address,
            name = name,
            rssi = result.rssi,
        )
        synchronized(loggedDeviceIds) {
            if (loggedDeviceIds.add(discovered.id)) {
                Log.d(TAG, "found ${discovered.name} (${discovered.id}), RSSI=${discovered.rssi}")
            }
        }
        WoliDeviceCenter.upsertDevice(discovered)
    }

    private fun handleStatusPayload(value: ByteArray) {
        val payload = value.toString(StandardCharsets.UTF_8)
        if (payload != lastLoggedStatusPayload) {
            Log.d(TAG, "status: $payload")
            lastLoggedStatusPayload = payload
        }
        WoliDeviceCenter.applyStatus(
            status = WoliDeviceProtocol.parseStatus(payload),
            rawPayload = payload,
        )
    }

    @SuppressLint("MissingPermission")
    private fun writeDescriptor(
        callbackGatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            callbackGatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = value
            callbackGatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    private fun failGatt(callbackGatt: BluetoothGatt, message: String) {
        Log.w(TAG, message)
        WoliDeviceCenter.markError(message)
        if (isCurrentGatt(callbackGatt)) {
            if (hasConnectPermission()) runCatching { callbackGatt.disconnect() }
            closeGatt(callbackGatt)
        } else {
            closeStaleGatt(callbackGatt)
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt(target: BluetoothGatt? = currentGatt()) {
        target ?: return
        if (hasConnectPermission()) runCatching { target.close() }
        val abandonedCommand = synchronized(operationLock) {
            if (gatt === target) {
                val pending = pendingCommand
                val queued = queuedCommand
                gatt = null
                commandCharacteristic = null
                statusCharacteristic = null
                connectedDevice = null
                pendingCommandGatt = null
                pendingCommand = null
                queuedCommand = null
                initialStatusReadGatt = null
                pending ?: queued
            } else {
                null
            }
        }
        abandonedCommand?.let { command ->
            WoliDeviceCenter.recordCommand(
                command,
                WoliDeviceActionResult.Failed("BLE 연결이 해제되어 명령을 완료하지 못했습니다."),
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeStaleGatt(target: BluetoothGatt) {
        if (hasConnectPermission()) runCatching { target.close() }
    }

    private fun currentGatt(): BluetoothGatt? = synchronized(operationLock) { gatt }

    private fun currentCommandCharacteristic(): BluetoothGattCharacteristic? = synchronized(operationLock) {
        commandCharacteristic
    }

    private fun currentStatusCharacteristic(): BluetoothGattCharacteristic? = synchronized(operationLock) {
        statusCharacteristic
    }

    private fun currentConnectedDevice(): WoliDiscoveredDevice? = synchronized(operationLock) { connectedDevice }

    private fun isCurrentGatt(candidate: BluetoothGatt): Boolean = synchronized(operationLock) { gatt === candidate }

    private fun isInitialStatusReadPending(candidate: BluetoothGatt): Boolean = synchronized(operationLock) {
        initialStatusReadGatt === candidate
    }

    private fun clearInitialStatusRead(candidate: BluetoothGatt): Boolean = synchronized(operationLock) {
        if (initialStatusReadGatt !== candidate) return@synchronized false
        initialStatusReadGatt = null
        true
    }

    private fun clearPendingCommand(candidate: BluetoothGatt, command: String) {
        synchronized(operationLock) {
            if (pendingCommandGatt === candidate && pendingCommand == command) {
                pendingCommandGatt = null
                pendingCommand = null
            }
        }
    }

    private fun dispatchQueuedCommand() {
        val command = synchronized(operationLock) {
            queuedCommand?.also { queuedCommand = null }
        } ?: return
        sendCommand(command)
    }

    private fun dispatchPendingFocusCommand() {
        val command = WoliFocusSessionController.pendingHardwareCommand.value ?: return
        val alreadyScheduled = isCommandScheduled(command)
        if (!alreadyScheduled) sendPendingFocusCommand()
    }

    private fun isCommandScheduled(command: String): Boolean = synchronized(operationLock) {
        pendingCommand == command || queuedCommand == command
    }

    private fun BluetoothGattCharacteristic.supportsWrite(): Boolean =
        properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0

    private fun BluetoothGattCharacteristic.supportsReadNotify(): Boolean =
        properties and BluetoothGattCharacteristic.PROPERTY_READ != 0 &&
            properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

    private fun applySimulatedCommand(command: String) {
        val current = WoliDeviceCenter.state.value.hardwareStatus
        val next = when (command) {
            WoliDeviceProtocol.COMMAND_LOCK -> current.copy(locked = true)
            WoliDeviceProtocol.COMMAND_UNLOCK -> current.copy(locked = false)
            WoliDeviceProtocol.COMMAND_START -> current.copy(locked = true, sessionActive = true)
            WoliDeviceProtocol.COMMAND_SESSION_END,
            WoliDeviceProtocol.COMMAND_STOP -> current.copy(locked = false, sessionActive = false)
            WoliDeviceProtocol.COMMAND_STATUS -> current
            else -> current
        }
        WoliDeviceCenter.applyStatus(next, WoliDeviceProtocol.encodeStatus(next))
    }
}
