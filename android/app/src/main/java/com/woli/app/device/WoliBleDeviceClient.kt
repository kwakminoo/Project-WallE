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
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.nio.charset.StandardCharsets

class WoliBleDeviceClient(private val context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter = bluetoothManager?.adapter

    companion object {
        private var gatt: BluetoothGatt? = null
        private var commandCharacteristic: BluetoothGattCharacteristic? = null
        private var connectedDevice: WoliDiscoveredDevice? = null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            WoliDeviceCenter.markError("BLE 검색 실패: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice?.let(WoliDeviceCenter::markConnected)
                if (hasConnectPermission()) {
                    runCatching { gatt.discoverServices() }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                commandCharacteristic = null
                WoliDeviceCenter.disconnect("월이 BLE 연결이 해제되었습니다.")
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(WoliDeviceProtocol.SERVICE_UUID)
            commandCharacteristic = service?.getCharacteristic(WoliDeviceProtocol.COMMAND_UUID)
            val statusCharacteristic = service?.getCharacteristic(WoliDeviceProtocol.STATUS_UUID)
            if (statusCharacteristic == null || !hasConnectPermission()) return

            runCatching {
                gatt.setCharacteristicNotification(statusCharacteristic, true)
                statusCharacteristic.getDescriptor(WoliDeviceProtocol.CLIENT_CONFIG_UUID)?.let { descriptor ->
                    writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                }
                gatt.readCharacteristic(statusCharacteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleStatusPayload(value)
        }

        @Deprecated("Deprecated Android callback retained for API compatibility")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleStatusPayload(characteristic.value ?: return)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) handleStatusPayload(value)
        }

        @Deprecated("Deprecated Android callback retained for API compatibility")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) handleStatusPayload(characteristic.value ?: return)
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
        if (!hasScanPermission()) return WoliDeviceActionResult.MissingPermission
        val scanner = bluetoothAdapter?.bluetoothLeScanner
            ?: return WoliDeviceActionResult.BluetoothUnavailable

        WoliDeviceCenter.markScanning()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        return runCatching {
            scanner.startScan(null, settings, scanCallback)
            WoliDeviceActionResult.Started
        }.getOrElse { error ->
            WoliDeviceCenter.markError(error.message ?: "BLE 검색을 시작하지 못했습니다.")
            WoliDeviceActionResult.Failed(error.message ?: "BLE 검색을 시작하지 못했습니다.")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!hasScanPermission()) return
        runCatching { bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback) }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WoliDiscoveredDevice): WoliDeviceActionResult {
        if (device.simulated) {
            WoliDeviceCenter.connectSimulated()
            return WoliDeviceActionResult.Started
        }
        if (!hasConnectPermission()) return WoliDeviceActionResult.MissingPermission
        val adapter = bluetoothAdapter ?: return WoliDeviceActionResult.BluetoothUnavailable

        stopScan()
        closeGatt()
        WoliDeviceCenter.markConnecting(device)
        connectedDevice = device

        return runCatching {
            val remoteDevice = adapter.getRemoteDevice(device.id)
            gatt = remoteDevice.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            WoliDeviceActionResult.Started
        }.getOrElse { error ->
            WoliDeviceCenter.markError(error.message ?: "월이 BLE 연결에 실패했습니다.")
            WoliDeviceActionResult.Failed(error.message ?: "월이 BLE 연결에 실패했습니다.")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (hasConnectPermission()) runCatching { gatt?.disconnect() }
        closeGatt()
        WoliDeviceCenter.disconnect()
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String): WoliDeviceActionResult {
        if (WoliDeviceCenter.state.value.connection == WoliDeviceConnectionState.Simulated) {
            applySimulatedCommand(command)
            return WoliDeviceActionResult.Sent
        }
        if (!hasConnectPermission()) return WoliDeviceActionResult.MissingPermission
        val activeGatt = gatt ?: return WoliDeviceActionResult.NotConnected
        val characteristic = commandCharacteristic ?: return WoliDeviceActionResult.NotConnected
        val payload = command.toByteArray(StandardCharsets.UTF_8)

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activeGatt.writeCharacteristic(
                    characteristic,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = payload
                @Suppress("DEPRECATION")
                activeGatt.writeCharacteristic(characteristic)
            }
            WoliDeviceActionResult.Sent
        }.getOrElse { error ->
            WoliDeviceActionResult.Failed(error.message ?: "명령 전송에 실패했습니다.")
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

        WoliDeviceCenter.upsertDevice(
            WoliDiscoveredDevice(
                id = device.address,
                name = name,
                rssi = result.rssi,
            ),
        )
    }

    private fun handleStatusPayload(value: ByteArray) {
        val payload = value.toString(StandardCharsets.UTF_8)
        WoliDeviceCenter.applyStatus(WoliDeviceProtocol.parseStatus(payload))
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        if (hasConnectPermission()) runCatching { gatt?.close() }
        gatt = null
        commandCharacteristic = null
        connectedDevice = null
    }

    @SuppressLint("MissingPermission")
    private fun writeDescriptor(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ) {
        if (!hasConnectPermission()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun applySimulatedCommand(command: String) {
        when (command) {
            WoliDeviceProtocol.COMMAND_LOCK,
            WoliDeviceProtocol.COMMAND_START -> WoliDeviceCenter.setLocked(true)
            WoliDeviceProtocol.COMMAND_UNLOCK,
            WoliDeviceProtocol.COMMAND_STOP -> WoliDeviceCenter.setLocked(false)
            WoliDeviceProtocol.COMMAND_STATUS -> Unit
        }
    }
}
