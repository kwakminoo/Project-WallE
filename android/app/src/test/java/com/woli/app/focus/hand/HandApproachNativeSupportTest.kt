package com.woli.app.focus.hand

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandApproachNativeSupportTest {
    @Test
    fun probeReturnsStableResult() {
        val first = HandApproachNativeSupport.isAvailable()
        val second = HandApproachNativeSupport.isAvailable()
        assertTrue(first == second)
        // JVM unit tests have no JNI; production devices with arm64 return true.
        assertFalse(first)
    }
}
