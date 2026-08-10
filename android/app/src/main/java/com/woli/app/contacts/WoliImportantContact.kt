package com.woli.app.contacts

import java.util.Locale
import java.util.UUID

data class WoliImportantContact(
    val id: String = "contact_${UUID.randomUUID()}",
    val displayName: String,
    val phoneNumber: String = "",
    val normalizedPhoneNumber: String = WoliPhoneNumberNormalizer.normalize(phoneNumber),
    val source: WoliImportantContactSource = WoliImportantContactSource.Manual,
    val enabled: Boolean = true,
) {
    val stableKey: String
        get() = normalizedPhoneNumber.ifBlank { displayName.trim().lowercase(Locale.KOREAN) }
}

enum class WoliImportantContactSource {
    Default,
    Device,
    Manual,
}

data class WoliSystemContact(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val normalizedPhoneNumber: String = WoliPhoneNumberNormalizer.normalize(phoneNumber),
)

object WoliPhoneNumberNormalizer {
    fun normalize(value: String?): String {
        val digits = value
            ?.filter(Char::isDigit)
            .orEmpty()

        return when {
            digits.startsWith("0082") -> "0" + digits.drop(4)
            digits.startsWith("82") && digits.length >= 10 -> "0" + digits.drop(2)
            else -> digits
        }
    }

    fun isComparable(value: String?): Boolean {
        return normalize(value).length >= 7
    }

    fun matches(left: String?, right: String?): Boolean {
        val a = normalize(left)
        val b = normalize(right)
        if (a.length < 7 || b.length < 7) return false

        return a == b || a.takeLast(8) == b.takeLast(8)
    }

    fun mask(value: String?): String {
        val normalized = normalize(value)
        if (normalized.length < 4) return "번호 미확인"
        return "끝자리 ${normalized.takeLast(4)}"
    }
}

object WoliImportantContactMatcher {
    fun matchText(text: String, contacts: List<WoliImportantContact>): WoliImportantContact? {
        val normalizedText = WoliPhoneNumberNormalizer.normalize(text)
        return contacts
            .filter { it.enabled }
            .firstOrNull { contact ->
                val nameMatched = contact.displayName.isNotBlank() &&
                    text.contains(contact.displayName, ignoreCase = true)
                val phoneMatched = contact.normalizedPhoneNumber.length >= 7 &&
                    normalizedText.contains(contact.normalizedPhoneNumber.takeLast(8))

                nameMatched || phoneMatched
            }
    }

    fun matchPhoneNumber(phoneNumber: String?, contacts: List<WoliImportantContact>): WoliImportantContact? {
        if (!WoliPhoneNumberNormalizer.isComparable(phoneNumber)) return null
        return contacts
            .filter { it.enabled && WoliPhoneNumberNormalizer.isComparable(it.normalizedPhoneNumber) }
            .firstOrNull { contact ->
                WoliPhoneNumberNormalizer.matches(phoneNumber, contact.normalizedPhoneNumber)
            }
    }
}
