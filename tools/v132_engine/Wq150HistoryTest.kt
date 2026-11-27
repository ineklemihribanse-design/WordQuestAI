package com.kelimebulmacasi.a1
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class Wq150HistoryTest {
    @Test fun fourPlayersAndRealScoresArePreserved(){
        val view=JSONObject("""{"phase":"finished","kind":"team","serverNow":123,"connection":{"players":[{"id":"a"},{"id":"b"},{"id":"c"},{"id":"d"}]},"summary":{"players":{"a":{"score":9},"b":{"score":-2},"c":{},"d":{"score":4}}}}""")
        val row=requireNotNull(Wq150History.entry("room",view,JSONObject("""{"b":{"name":"Friend"}}"""),"a","Me"))
        val players=row.getJSONArray("players")
        assertEquals(4,players.length())
        assertEquals(-2,players.getJSONObject(1).getInt("score"))
        assertEquals("Friend",players.getJSONObject(1).getString("name"))
        assertFalse(players.getJSONObject(2).has("score"))
        assertEquals("team",row.getString("kind"))
        view.put("phase","playing")
        assertNull(Wq150History.entry("room",view,JSONObject(),"a","Me"))
    }
}
