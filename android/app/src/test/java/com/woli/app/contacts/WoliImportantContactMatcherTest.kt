package com.woli.app.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WoliImportantContactMatcherTest {
    @Test
    fun phoneNumbersMatchByKoreanNormalizedSuffix() {
        val contacts = listOf(
            WoliImportantContact(
                displayName = "어머니",
                phoneNumber = "010-1234-5678",
            ),
        )

        val matched = WoliImportantContactMatcher.matchPhoneNumber(
            phoneNumber = "+82 10 1234 5678",
            contacts = contacts,
        )

        assertEquals("어머니", matched?.displayName)
    }

    @Test
    fun notificationTextMatchesEnabledContactName() {
        val contacts = listOf(
            WoliImportantContact(displayName = "담임 선생님", enabled = true),
            WoliImportantContact(displayName = "광고", enabled = false),
        )

        val matched = WoliImportantContactMatcher.matchText(
            text = "담임 선생님: 오늘 일정 확인해주세요.",
            contacts = contacts,
        )

        assertEquals("담임 선생님", matched?.displayName)
        assertNull(WoliImportantContactMatcher.matchText("광고 메시지", contacts))
    }
}
