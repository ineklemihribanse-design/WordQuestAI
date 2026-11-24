package com.kelimebulmacasi.a1

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class Wq210LenaRouterTest {
    @Test fun unavailableProvidersSkipped() = runBlocking {
        val router = Wq210LenaRouter { 0L }
        val seen = mutableListOf<Wq210LenaService>()
        val keys = mapOf(Wq210LenaService.Groq to "g", Wq210LenaService.Gemini to "x", Wq210LenaService.OpenRouter to "r")
        val answer = router.run(keys) { provider, key ->
            seen.add(provider)
            if (provider == Wq210LenaService.Groq) {
                assertEquals("g", key)
                throw Wq210HttpFailure(429,120)
            }
            assertEquals("x",key)
            "ok"
        }
        assertEquals("ok",answer)
        assertEquals(listOf(Wq210LenaService.Groq,Wq210LenaService.Gemini),seen)
        assertEquals(listOf(Wq210LenaService.Gemini,Wq210LenaService.OpenRouter),router.eligible(keys))
    }
    @Test fun missingKeySkippedAndRetryAfterRespected() = runBlocking {
        var time = 0L
        val router = Wq210LenaRouter { time }
        val keys = mapOf(Wq210LenaService.Groq to "", Wq210LenaService.Gemini to "x", Wq210LenaService.OpenRouter to "r")
        val response = router.run(keys) { service, _ ->
            if (service == Wq210LenaService.Gemini) throw Wq210HttpFailure(503,120)
            assertEquals(Wq210LenaService.OpenRouter,service)
            "fallback"
        }
        assertEquals("fallback",response)
        time = 119999
        assertEquals(listOf(Wq210LenaService.OpenRouter),router.eligible(keys))
        time = 120000
        assertEquals(listOf(Wq210LenaService.Gemini,Wq210LenaService.OpenRouter),router.eligible(keys))
    }
    @Test fun openRouterFixedFreeModel() {
        assertEquals("openrouter/free",Wq210LenaService.OpenRouter.model)
        assertEquals(0L,wq210CooldownMillis(429,0)-60000L)
        assertEquals(120000L,wq210CooldownMillis(503,120))
    }
}
