package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq150LobbyClientTest {
    @Test fun routesStayOnConfiguredOrigin(){
        val e=Wq150Endpoint("https://arena.example.org")
        assertEquals("https://arena.example.org/v150/lobbies",e.createUrl().toString())
        assertEquals("https://arena.example.org/v150/lobbies/room123",e.lobbyUrl("room123").toString())
        assertEquals("https://arena.example.org/v150/lobbies/room123/commands",e.lobbyUrl("room123",true).toString())
        assertTrue(runCatching{e.lobbyUrl("../bad")}.isFailure)
    }
    @Test fun joiningDoesNotRequirePrivateRevision(){
        assertTrue(Wq150LobbyCommand("join").valid())
        assertFalse(Wq150LobbyCommand("start").valid())
        assertFalse(Wq150LobbyCommand("join",ready=true).valid())
    }
    @Test fun readyNeedsBothRevisionsAndBoolean(){
        val c=Wq150LobbyCommand("ready",2,1,true)
        assertTrue(c.valid());assertEquals(c.id,c.copy().id)
        assertFalse(c.copy(configRevision=null).valid())
        assertFalse(c.copy(ready=null).valid())
        assertFalse(c.copy(revision=-1).valid())
    }
    @Test fun customValuesKeepExistingServerBounds(){
        val s=Wq150Settings(count=27,seconds=37,hints=7,retries=2,skipLimit=4)
        assertTrue(s.valid())
        assertTrue(Wq150LobbyCommand("settings",2,settings=s).valid())
        assertEquals(27,s.changeKind("quiz").count)
        assertFalse(s.copy(count=71).valid())
        assertFalse(s.copy(seconds=601).valid())
        assertFalse(s.copy(hints=21).valid())
        assertFalse(s.copy(retries=4).valid())
        assertFalse(s.copy(skipLimit=6).valid())
        assertTrue(s.copy(kind="coop",mode="timed",seconds=181,maxMistakes=9).valid())
    }
    @Test fun onlyValidSettingsMayBeSent(){
        assertTrue(Wq150LobbyCommand("settings",2,settings=Wq150Settings()).valid())
        assertFalse(Wq150LobbyCommand("settings",2,settings=Wq150Settings(count=71)).valid())
        assertFalse(Wq150LobbyCommand("start",2,settings=Wq150Settings()).valid())
    }
    @Test fun quotaAndExplicitRejectionsDoNotLockLobby(){
        assertTrue(wq150LobbyFinalError(429,"room-quota"))
        assertTrue(wq150LobbyFinalError(400,"invalid-settings"))
        assertTrue(wq150LobbyFinalError(409,"lobby-not-ready"))
        assertTrue(wq150LobbyFinalError(503,"not-configured"))
        assertFalse(wq150LobbyFinalError(409,"refresh-required"))
        assertFalse(wq150LobbyFinalError(502,"refresh-required"))
        assertTrue(wq150LobbyErrorText(429,"room-quota").contains("10"))
    }
}
