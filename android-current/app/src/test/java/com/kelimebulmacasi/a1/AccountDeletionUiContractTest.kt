package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionUiContractTest {
    @Test fun confirmationIsTwoButtonsOnly() {
        assertEquals("Hesabını kalıcı olarak silmeyi onaylıyor musun?", AccountDeletionCopy.prompt)
        assertEquals("Evet, sil", AccountDeletionCopy.confirm)
        assertEquals("Vazgeç", AccountDeletionCopy.cancel)
    }
    @Test fun competitorNameHasSafeFallback() {
        assertEquals("Çağla", ArenaNickname.safeDisplay("Çağla", "Misafir"))
        assertFalse(ArenaNickname.isValid("A\nB"))
    }
}
