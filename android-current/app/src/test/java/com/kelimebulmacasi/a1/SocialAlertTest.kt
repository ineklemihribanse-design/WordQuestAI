package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SocialAlertTest {
    @Test fun separateFriendAndRoomAlertsHaveDifferentIds() {
        val friend = SocialAlert(SocialAlertType.FRIEND_REQUEST, "sender", "Arkadaş", 101L)
        val room = SocialAlert(SocialAlertType.ROOM_INVITE, "sender", "Arkadaş", 101L, "123456789")
        assertNotEquals(friend.key, room.key)
    }
    @Test fun nextRequestCanNotifyAfterPreviousOneWasDismissed() {
        val first = SocialAlert(SocialAlertType.ROOM_INVITE, "sender", "A", 101L, "123456789")
        val newer = first.copy(timestamp = 102L)
        assertNotEquals(first.key, newer.key)
        assertEquals("123456789", newer.roomCode)
    }
}
