package com.kelimebulmacasi.a1

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class Wq208ProviderFailureTest {
    @Test fun quotaNeverRetries() {
        val failure = wq208ProviderFailure("OpenAI", 429, "insufficient_quota", "", "1")
        assertNull(failure.retryDelayMs)
        assertTrue(failure.message.orEmpty().contains("bütçesi"))
    }
    @Test fun ambiguous429NeverRetries() {
        assertNull(wq208ProviderFailure("OpenAI", 429, "", "", null).retryDelayMs)
    }
    @Test fun explicitRateLimitRespectsRetryAfter() {
        assertTrue(wq208ProviderFailure("OpenAI", 429, "rate_limit_exceeded", "", "2").retryDelayMs!! >= 2000)
        assertNull(wq208ProviderFailure("OpenAI", 429, "rate_limit_exceeded", "", "30").retryDelayMs)
    }
    @Test fun temporary503RetriesOnce() = runBlocking {
        val calls = AtomicInteger()
        val reply = wq207LenaRequest(12000) {
            if (calls.incrementAndGet() == 1) throw wq208ProviderFailure("Gemini", 503, "", "", null)
            "hello"
        }
        assertEquals("hello", reply)
        assertEquals(2, calls.get())
    }
    @Test fun prolonged503DoesNotRetry() = runBlocking {
        val calls = AtomicInteger()
        try {
            wq207LenaRequest<String>(12000) {
                calls.incrementAndGet()
                throw wq208ProviderFailure("Gemini", 503, "", "", "60")
            }
            fail("expected provider failure")
        } catch (e: WordRequestException) {
            assertTrue(e.message.orEmpty().contains("yoğun"))
        }
        assertEquals(1, calls.get())
    }
    @Test fun repeated503StopsAfterSecondAttempt() = runBlocking {
        val calls = AtomicInteger()
        try {
            wq207LenaRequest<String>(12000) {
                calls.incrementAndGet()
                throw wq208ProviderFailure("Gemini", 503, "", "", null)
            }
            fail("expected provider failure")
        } catch (e: WordRequestException) {
            assertTrue(e.message.orEmpty().contains("yoğun"))
        }
        assertEquals(2, calls.get())
    }
}
