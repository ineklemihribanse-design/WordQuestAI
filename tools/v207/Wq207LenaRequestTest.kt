package com.kelimebulmacasi.a1

import java.net.ConnectException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class Wq207LenaRequestTest {
    @Test fun quickReplyPreserved() = runBlocking {
        assertEquals("hello", wq207LenaRequest(3000) { "hello" })
    }

    @Test fun connectionRefusedRetriedOnce() = runBlocking {
        val attempts = AtomicInteger()
        val result = wq207LenaRequest(3000) {
            if (attempts.incrementAndGet() == 1) throw ConnectException("refused")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(2, attempts.get())
    }

    @Test fun deadlineDoesNotWaitForWorker() = runBlocking {
        val started = System.nanoTime()
        try {
            wq207LenaRequest(120) { Thread.sleep(1800); "late" }
            fail("late response accepted")
        } catch (e: WordRequestException) {
            assertTrue(e.message.orEmpty().contains("zamanında"))
        }
        assertTrue((System.nanoTime() - started) / 1_000_000 < 1500)
    }

    @Test fun invalidKeyIsNotRetried() = runBlocking {
        val attempts = AtomicInteger()
        try {
            wq207LenaRequest<String>(3000) {
                attempts.incrementAndGet()
                throw WordRequestException("invalid", invalidKey = true)
            }
            fail("invalid key accepted")
        } catch (e: WordRequestException) {
            assertTrue(e.invalidKey)
        }
        assertEquals(1, attempts.get())
    }
}
