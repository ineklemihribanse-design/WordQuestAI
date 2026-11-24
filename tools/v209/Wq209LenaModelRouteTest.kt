package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.runBlocking

class Wq209LenaModelRouteTest {
    @Test fun oldDefaultMovesToNewLite() {
        assertEquals("gemini-3.5-flash-lite", Wq209LenaModelRoute(AiProvider.Gemini,"gemini-3.1-flash-lite").model)
    }
    @Test fun retryUsesDifferentModel() = runBlocking {
        val route = Wq209LenaModelRoute(AiProvider.Gemini,"gemini-3.1-flash-lite")
        val tried = mutableListOf<String>()
        val answer = wq207LenaRequest(12000) {
            tried.add(route.model)
            if (tried.size == 1) {
                val f = wq208ProviderFailure("Google Gemini",503,"","","1")
                route.onFailure(f)
                throw f
            }
            "hello"
        }
        assertEquals("hello",answer)
        assertEquals(listOf("gemini-3.5-flash-lite","gemini-3.1-flash-lite"),tried)
    }
    @Test fun quotaAndLongBackoffNeverSwitch() {
        val route = Wq209LenaModelRoute(AiProvider.Gemini,"gemini-3.6-flash")
        route.onFailure(wq208ProviderFailure("Google Gemini",429,"","","1"))
        route.onFailure(wq208ProviderFailure("Google Gemini",503,"","","60"))
        assertEquals("gemini-3.6-flash",route.model)
    }
    @Test fun openAiNeverSwitchesToGemini() {
        val route = Wq209LenaModelRoute(AiProvider.OpenAI,"gpt-4.1-mini")
        route.onFailure(wq208ProviderFailure("OpenAI",503,"","",null))
        assertEquals("gpt-4.1-mini",route.model)
    }
}
