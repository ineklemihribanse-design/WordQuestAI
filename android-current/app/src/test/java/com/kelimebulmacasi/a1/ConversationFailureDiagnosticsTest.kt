package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class ConversationFailureDiagnosticsTest {
    @Test fun distinguishDnsFromOtherConnectionFailures() {
        val msg = conversationFailureMessage(java.net.UnknownHostException("private-token-marker"))
        assertTrue(msg.contains("DNS"))
        assertFalse(msg.contains("private-token-marker"))
        assertTrue(conversationFailureMessage(java.net.ConnectException()).contains("sunucusuna"))
    }

    @Test fun distinguishTransportAndUnexpectedErrorsWithoutSecrets() {
        assertTrue(conversationFailureMessage(java.net.SocketTimeoutException()).contains("zaman aşımı"))
        val failure = conversationFailureMessage(IllegalStateException("MY-SECRET-API-KEY"))
        assertTrue(failure.contains("IllegalStateException"))
        assertFalse(failure.contains("MY-SECRET-API-KEY"))
    }
}
