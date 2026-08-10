package com.woli.app.contacts

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract

object WoliSystemContactReader {
    private const val MAX_IMPORTABLE_CONTACTS = 100

    @SuppressLint("Range", "MissingPermission")
    fun readPhoneContacts(context: Context): WoliSystemContactReadResult {
        if (!WoliContactsAccess.canReadContacts(context)) {
            return WoliSystemContactReadResult.MissingPermission
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )

        return runCatching {
            val contacts = mutableListOf<WoliSystemContact>()
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC",
            )?.use { cursor ->
                while (cursor.moveToNext() && contacts.size < MAX_IMPORTABLE_CONTACTS) {
                    val id = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                    ).orEmpty()
                    val displayName = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY),
                    ).orEmpty()
                    val phoneNumber = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    ).orEmpty()
                    val normalized = WoliPhoneNumberNormalizer.normalize(phoneNumber)
                    if (displayName.isBlank() || normalized.length < 7) continue

                    contacts += WoliSystemContact(
                        id = id.ifBlank { normalized },
                        displayName = displayName,
                        phoneNumber = phoneNumber,
                        normalizedPhoneNumber = normalized,
                    )
                }
            }

            WoliSystemContactReadResult.Success(
                contacts.distinctBy { it.normalizedPhoneNumber },
            )
        }.getOrElse { error ->
            WoliSystemContactReadResult.Failed(
                error.message ?: "연락처를 불러오지 못했습니다.",
            )
        }
    }
}

sealed class WoliSystemContactReadResult {
    data class Success(val contacts: List<WoliSystemContact>) : WoliSystemContactReadResult()
    data object MissingPermission : WoliSystemContactReadResult()
    data class Failed(val reason: String) : WoliSystemContactReadResult()

    fun userMessage(): String {
        return when (this) {
            is Success -> "${contacts.size}개의 연락처를 불러왔습니다."
            MissingPermission -> "연락처 권한이 필요합니다."
            is Failed -> reason
        }
    }
}
