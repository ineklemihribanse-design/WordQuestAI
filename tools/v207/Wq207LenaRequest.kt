package com.kelimebulmacasi.a1

import java.net.ConnectException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The UI must not wait for a blocking HTTP read after its response deadline.
 * The IO worker owns only the network request, never chat history or UI state.
 * A cancelled continuation cannot deliver a late reply to AiSession.
 */
internal suspend fun <T : Any> wq207LenaRequest(
    timeoutMs: Long = 22_000L,
    request: suspend () -> T,
): T {
    return withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<T> { continuation ->
            val worker = CoroutineScope(Dispatchers.IO).launch {
                val outcome = runCatching {
                    try {
                        request()
                    } catch (_: ConnectException) {
                        // Connection refused before submission: retry once only.
                        // Never retry a timeout, HTTP response, invalid key or paid request.
                        delay(250)
                        request()
                    }
                }
                continuation.resumeWith(outcome)
            }
            continuation.invokeOnCancellation { worker.cancel() }
        }
    } ?: throw WordRequestException(
        "Lena zamanında yanıt veremedi. Mesajın korunuyor; tekrar gönderebilirsin."
    )
}
