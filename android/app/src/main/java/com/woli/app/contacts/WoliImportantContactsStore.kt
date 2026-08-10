package com.woli.app.contacts

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object WoliImportantContactsStore {
    private const val PREFS_NAME = "woli_important_contacts"
    private const val KEY_CONTACTS_JSON = "contacts_json"
    private const val MAX_CONTACTS = 50

    private val lock = Any()
    private val _contacts = MutableStateFlow<List<WoliImportantContact>>(emptyList())

    @Volatile
    private var loaded = false

    @Volatile
    private var persistenceContext: Context? = null

    val contacts: StateFlow<List<WoliImportantContact>> = _contacts.asStateFlow()

    fun load(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            persistenceContext = appContext
            if (loaded) return

            val saved = readContacts(appContext)
            _contacts.value = saved.ifEmpty { defaultContacts() }
            loaded = true
            persistLocked()
        }
    }

    fun enabledContactsSnapshot(): List<WoliImportantContact> {
        return _contacts.value.filter { it.enabled }
    }

    fun upsert(context: Context, contact: WoliImportantContact) {
        load(context)
        synchronized(lock) {
            val normalized = contact.copy(
                displayName = contact.displayName.trim(),
                phoneNumber = contact.phoneNumber.trim(),
                normalizedPhoneNumber = WoliPhoneNumberNormalizer.normalize(contact.phoneNumber),
            )
            if (normalized.displayName.isBlank() && normalized.normalizedPhoneNumber.isBlank()) return

            val withoutDuplicate = _contacts.value.filterNot { existing ->
                existing.id == normalized.id ||
                    (
                        normalized.normalizedPhoneNumber.isNotBlank() &&
                            existing.normalizedPhoneNumber == normalized.normalizedPhoneNumber
                        )
            }
            _contacts.value = (listOf(normalized) + withoutDuplicate)
                .sortedBy { if (it.enabled) 0 else 1 }
                .take(MAX_CONTACTS)
            persistLocked()
        }
    }

    fun importDeviceContacts(context: Context, contacts: List<WoliSystemContact>) {
        load(context)
        synchronized(lock) {
            val converted = contacts
                .filter { it.displayName.isNotBlank() && it.normalizedPhoneNumber.isNotBlank() }
                .map {
                    WoliImportantContact(
                        id = "device_${it.normalizedPhoneNumber}",
                        displayName = it.displayName.trim(),
                        phoneNumber = it.phoneNumber.trim(),
                        normalizedPhoneNumber = it.normalizedPhoneNumber,
                        source = WoliImportantContactSource.Device,
                        enabled = true,
                    )
                }

            val next = (converted + _contacts.value)
                .distinctBy { it.stableKey }
                .take(MAX_CONTACTS)
            _contacts.value = next
            persistLocked()
        }
    }

    fun toggleEnabled(context: Context, id: String) {
        load(context)
        synchronized(lock) {
            _contacts.value = _contacts.value.map { contact ->
                if (contact.id == id) contact.copy(enabled = !contact.enabled) else contact
            }
            persistLocked()
        }
    }

    fun remove(context: Context, id: String) {
        load(context)
        synchronized(lock) {
            _contacts.value = _contacts.value.filterNot { it.id == id }
            persistLocked()
        }
    }

    fun resetForTest() {
        synchronized(lock) {
            _contacts.value = emptyList()
            persistenceContext = null
            loaded = false
        }
    }

    private fun defaultContacts(): List<WoliImportantContact> {
        return listOf(
            WoliImportantContact(
                id = "default_mother",
                displayName = "어머니",
                source = WoliImportantContactSource.Default,
            ),
            WoliImportantContact(
                id = "default_father",
                displayName = "아버지",
                source = WoliImportantContactSource.Default,
            ),
        )
    }

    private fun readContacts(context: Context): List<WoliImportantContact> {
        val json = prefs(context).getString(KEY_CONTACTS_JSON, null).orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val displayName = item.optString("displayName").trim()
                    val phoneNumber = item.optString("phoneNumber").trim()
                    if (displayName.isBlank() && phoneNumber.isBlank()) continue

                    add(
                        WoliImportantContact(
                            id = item.optString("id").ifBlank { "contact_$index" },
                            displayName = displayName,
                            phoneNumber = phoneNumber,
                            normalizedPhoneNumber = WoliPhoneNumberNormalizer.normalize(
                                item.optString("normalizedPhoneNumber").ifBlank { phoneNumber },
                            ),
                            source = item.optString("source").toSource(),
                            enabled = item.optBoolean("enabled", true),
                        ),
                    )
                }
            }.take(MAX_CONTACTS)
        }.getOrDefault(emptyList())
    }

    private fun persistLocked() {
        val context = persistenceContext ?: return
        val array = JSONArray()
        _contacts.value.forEach { contact ->
            array.put(
                JSONObject()
                    .put("id", contact.id)
                    .put("displayName", contact.displayName)
                    .put("phoneNumber", contact.phoneNumber)
                    .put("normalizedPhoneNumber", contact.normalizedPhoneNumber)
                    .put("source", contact.source.name)
                    .put("enabled", contact.enabled),
            )
        }
        prefs(context).edit()
            .putString(KEY_CONTACTS_JSON, array.toString())
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun String.toSource(): WoliImportantContactSource {
        return runCatching { WoliImportantContactSource.valueOf(this) }
            .getOrDefault(WoliImportantContactSource.Manual)
    }
}
