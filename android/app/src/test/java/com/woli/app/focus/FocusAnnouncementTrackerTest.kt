package com.woli.app.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusAnnouncementTrackerTest {
    @Test
    fun notificationAndCallAnnounceOncePerSession() {
        FocusAnnouncementTracker.reset()
        assertTrue(FocusAnnouncementTracker.shouldAnnounceNotification("n1"))
        FocusAnnouncementTracker.markNotificationAnnounced("n1")
        assertFalse(FocusAnnouncementTracker.shouldAnnounceNotification("n1"))

        assertTrue(FocusAnnouncementTracker.shouldAnnounceCall("c1"))
        FocusAnnouncementTracker.markCallAnnounced("c1")
        assertFalse(FocusAnnouncementTracker.shouldAnnounceCall("c1"))

        FocusAnnouncementTracker.reset()
        assertTrue(FocusAnnouncementTracker.shouldAnnounceNotification("n1"))
        assertTrue(FocusAnnouncementTracker.shouldAnnounceCall("c1"))
    }
}
