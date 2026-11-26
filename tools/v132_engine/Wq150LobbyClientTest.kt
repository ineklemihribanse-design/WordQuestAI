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
    @Test fun onlyValidSettingsMayBeSent(){
        assertTrue(Wq150LobbyCommand("settings",2,settings=Wq150Settings()).valid())
        assertFalse(Wq150LobbyCommand("settings",2,settings=Wq150Settings(count=71)).valid())
        assertFalse(Wq150LobbyCommand("start",2,settings=Wq150Settings()).valid())
    }
}
