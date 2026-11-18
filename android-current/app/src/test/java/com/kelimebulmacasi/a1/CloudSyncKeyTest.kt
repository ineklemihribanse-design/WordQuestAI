package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncKeyTest {
    @Test fun firebaseKeysCannotContainForbiddenCharacters() {
        val key = CloudSyncKey.forProgress("game/id.with#special[$]chars", "my-device")
        assertTrue(key.matches(Regex("[a-f0-9]{64}_[a-f0-9]{16}")))
        assertFalse(key.contains('/'))
        assertFalse(key.contains('.'))
    }

    @Test fun recordsFromDifferentDevicesCannotOverwriteEachOther() {
        val first = CloudSyncKey.forProgress("puzzle-1", "phone-a")
        val second = CloudSyncKey.forProgress("puzzle-1", "phone-b")
        assertNotEquals(first, second)
        assertEquals(first, CloudSyncKey.forProgress("puzzle-1", "phone-a"))
    }

    @Test fun syncMessagesNeverPretendCloudIsUpWhenOffline() {
        assertTrue(CloudSyncMessage.offline.contains("cihazda"))
        assertTrue(CloudSyncMessage.noPermission.contains("Firebase"))
        assertNotEquals(CloudSyncMessage.pending, CloudSyncMessage.ready)
    }
}
