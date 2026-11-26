package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test

class Wq150GatewayTest {
    @Test fun endpointRequiresExplicitHttpsOrigin(){
        val e=Wq150Endpoint("https://arena.example.org")
        assertEquals("https://arena.example.org/v150/matches/room123/commands",e.matchUrl("room123",true).toString())
        listOf("http://arena.example.org","https://user@arena.example.org","https://arena.example.org/path",
            "https://arena.example.org?token=secret","https://arena.example.org:8080").forEach{
            assertTrue(runCatching{Wq150Endpoint(it)}.isFailure)
        }
        assertTrue(runCatching{e.matchUrl("../bad")}.isFailure)
    }
    @Test fun validatesCommandsAndStableRetryId(){
        val c=Wq150Command(2,"submit",word=0,answer="APPLE")
        assertTrue(c.valid());assertEquals(c.id,c.copy().id)
        assertFalse(c.copy(answer="").valid())
        assertFalse(c.copy(choice=1).valid())
        assertFalse(c.copy(index=1).valid())
        assertFalse(c.copy(revision=-1).valid())
        assertFalse(c.copy(type="leave").valid())
        assertFalse(c.copy(id="__proto__").valid())
        assertTrue(Wq150Command(0,"answer",index=0,choice=4).valid())
        assertFalse(Wq150Command(0,"answer",index=0,choice=5).valid())
        assertTrue(Wq150Command(0,"hint",item=1).valid())
    }
}
