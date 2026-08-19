package com.woli.app.notification

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object WoliNotificationRuleStore {
    private const val PREFS_NAME = "woli_notification_rules"
    private const val KEY_CONFIG_JSON = "config_json"

    private val lock = Any()
    private val _config = MutableStateFlow(WoliNotificationRuleConfig())

    @Volatile
    private var loaded = false

    @Volatile
    private var persistenceContext: Context? = null

    val config: StateFlow<WoliNotificationRuleConfig> = _config.asStateFlow()

    fun load(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            persistenceContext = appContext
            if (loaded) return

            _config.value = readConfig(appContext)
            loaded = true
            persistLocked()
        }
    }

    fun configSnapshot(context: Context): WoliNotificationRuleConfig {
        load(context)
        return _config.value
    }

    fun updateMode(context: Context, mode: WoliNotificationMode) {
        load(context)
        synchronized(lock) {
            _config.value = _config.value.copy(mode = mode)
            persistLocked()
        }
    }

    fun setAllowedTarget(
        context: Context,
        targetId: String,
        enabled: Boolean,
    ) {
        val target = WoliNotificationRuleCatalog.targetById(targetId) ?: return
        load(context)
        synchronized(lock) {
            val current = _config.value.allowedPackageNames
            val nextAllowed = if (enabled) {
                current + target.packageNames
            } else {
                current - target.packageNames
            }
            _config.value = _config.value.copy(allowedPackageNames = nextAllowed)
            persistLocked()
        }
    }

    fun resetToDefault(context: Context) {
        load(context)
        synchronized(lock) {
            _config.value = WoliNotificationRuleConfig()
            persistLocked()
        }
    }

    fun resetForTest() {
        synchronized(lock) {
            _config.value = WoliNotificationRuleConfig()
            persistenceContext = null
            loaded = false
        }
    }

    private fun readConfig(context: Context): WoliNotificationRuleConfig {
        val json = prefs(context).getString(KEY_CONFIG_JSON, null).orEmpty()
        if (json.isBlank()) return WoliNotificationRuleConfig()

        return runCatching {
            val root = JSONObject(json)
            WoliNotificationRuleConfig(
                mode = root.optString("mode").toMode(),
                allowedPackageNames = root.optJSONArray("allowedPackageNames").toStringSet(),
                blockedPackageNames = root.optJSONArray("blockedPackageNames").toStringSet(),
                extraUrgentKeywords = root.optJSONArray("extraUrgentKeywords").toStringSet(),
            )
        }.getOrDefault(WoliNotificationRuleConfig())
    }

    private fun persistLocked() {
        val context = persistenceContext ?: return
        val config = _config.value
        val root = JSONObject()
            .put("mode", config.mode.name)
            .put("allowedPackageNames", config.allowedPackageNames.toJsonArray())
            .put("blockedPackageNames", config.blockedPackageNames.toJsonArray())
            .put("extraUrgentKeywords", config.extraUrgentKeywords.toJsonArray())

        prefs(context).edit()
            .putString(KEY_CONFIG_JSON, root.toString())
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun String.toMode(): WoliNotificationMode {
        return runCatching { WoliNotificationMode.valueOf(this) }
            .getOrDefault(WoliNotificationMode.Balanced)
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet {
            for (index in 0 until length()) {
                val value = optString(index).trim()
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun Set<String>.toJsonArray(): JSONArray {
        val array = JSONArray()
        sorted().forEach(array::put)
        return array
    }
}
