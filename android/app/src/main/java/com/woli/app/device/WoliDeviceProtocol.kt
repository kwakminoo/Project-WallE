package com.woli.app.device

import java.util.UUID

object WoliDeviceProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("7e7a0001-1f5f-4c2b-9b6f-2d1b7f4f0100")
    val COMMAND_UUID: UUID = UUID.fromString("7e7a0002-1f5f-4c2b-9b6f-2d1b7f4f0100")
    val STATUS_UUID: UUID = UUID.fromString("7e7a0003-1f5f-4c2b-9b6f-2d1b7f4f0100")
    val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val DEVICE_NAME_PREFIX = "WOLI"

    const val COMMAND_LOCK = "LOCK"
    const val COMMAND_UNLOCK = "UNLOCK"
    const val COMMAND_START = "START"
    const val COMMAND_STOP = "STOP"
    const val COMMAND_STATUS = "STATUS"

    fun commandCalibrateLock(angle: Int): String = "CAL_LOCK=${angle.coerceIn(0, 180)}"

    fun commandCalibrateUnlock(angle: Int): String = "CAL_UNLOCK=${angle.coerceIn(0, 180)}"

    fun parseStatus(payload: String): WoliDeviceHardwareStatus {
        val pairs = payload
            .split(';', '\n', ',')
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                part.substring(0, separator).trim().lowercase() to
                    part.substring(separator + 1).trim()
            }
            .toMap()

        return WoliDeviceHardwareStatus(
            mounted = pairs["mount"].asBoolean(),
            locked = pairs["lock"].asBoolean(),
            handNear = pairs["hand"].asBoolean(),
            batteryPercent = pairs["battery"]?.toIntOrNull()?.coerceIn(0, 100),
        )
    }

    fun encodeStatus(status: WoliDeviceHardwareStatus): String {
        return listOf(
            "mount=${status.mounted.asInt()}",
            "lock=${status.locked.asInt()}",
            "hand=${status.handNear.asInt()}",
            "battery=${status.batteryPercent ?: 100}",
        ).joinToString(";")
    }

    private fun String?.asBoolean(): Boolean {
        return this == "1" || equals("true", ignoreCase = true) || equals("yes", ignoreCase = true)
    }

    private fun Boolean.asInt(): Int = if (this) 1 else 0
}

data class WoliDeviceHardwareStatus(
    val mounted: Boolean = false,
    val locked: Boolean = false,
    val handNear: Boolean = false,
    val batteryPercent: Int? = null,
)
