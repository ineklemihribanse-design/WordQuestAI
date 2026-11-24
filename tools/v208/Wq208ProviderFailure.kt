package com.kelimebulmacasi.a1

import java.io.IOException
import kotlin.random.Random

internal class Wq208ProviderFailure(
    message: String, val retryDelayMs: Long?
) : IOException(message)

internal fun wq208ProviderFailure(
    provider: String, status: Int, code: String, type: String, retryAfter: String?
): Wq208ProviderFailure {
    val quota = code.equals("insufficient_quota", true) || type.equals("insufficient_quota", true) ||
        code.equals("billing_hard_limit_reached", true)
    val wait = if (retryAfter.isNullOrBlank()) null else
        retryAfter.trim().toLongOrNull()?.takeIf { it in 0..3 }?.times(1000)
    val shortWait = retryAfter.isNullOrBlank() || wait != null
    val delay = when {
        quota -> null
        status == 503 && shortWait -> maxOf(1000L, wait ?: 1000L) + Random.nextLong(0, 251)
        status == 429 && code.equals("rate_limit_exceeded", true) && wait != null ->
            maxOf(1000L, wait) + Random.nextLong(0, 251)
        else -> null
    }
    val message = when {
        status == 429 && quota -> "$provider: API kotası veya bütçesi dolmuş. API hesabının kullanım ve ödeme ayarlarını kontrol et. Mesajın korunuyor."
        status == 429 && code.equals("rate_limit_exceeded", true) -> "$provider: İstek hızı sınırına ulaşıldı. Biraz sonra tekrar gönder. Mesajın korunuyor."
        status == 429 -> "$provider: API kullanım sınırına ulaşıldı. Kota ve hız sınırını kontrol et. Mesajın korunuyor."
        else -> "$provider: Sunucu geçici olarak yoğun. Mesajın korunuyor; yeniden gönderebilirsin."
    }
    return Wq208ProviderFailure(message, delay)
}
