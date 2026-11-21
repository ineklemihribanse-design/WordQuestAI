package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq180FriendActivityTest {
    @Test fun missingActivityNeverPretendsOnline() {
        assertEquals("Son etkinlik paylaşılmadı",Wq180FriendActivity.label(null,200000L))
        assertEquals("Son etkinlik paylaşılmadı",Wq180FriendActivity.label(300000L,200000L))
    }
    @Test fun lastSeenIntervalsAreFactual() {
        assertEquals("Az önce aktifti",Wq180FriendActivity.label(100000L,190000L))
        assertEquals("5 dk önce aktifti",Wq180FriendActivity.label(200000L,500000L))
        assertEquals("2 sa önce aktifti",Wq180FriendActivity.label(100000L,7300000L))
    }
}
