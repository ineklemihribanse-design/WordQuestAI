package com.kelimebulmacasi.a1

import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BritishLocalePolicyTest {
    @Test fun britishLocaleAndLegacyRegionAliasesAreAccepted() {
        assertTrue(BritishLocalePolicy.isBritish(Locale.UK))
        assertTrue(BritishLocalePolicy.isBritish(Locale("en", "UK")))
        assertTrue(BritishLocalePolicy.isBritish(Locale("eng", "GBR")))
    }

    @Test fun americanAustralianAndOtherLanguagesNeverCountAsBritish() {
        assertFalse(BritishLocalePolicy.isBritish(Locale.US))
        assertFalse(BritishLocalePolicy.isBritish(Locale("en", "AU")))
        assertFalse(BritishLocalePolicy.isBritish(Locale("es", "GB")))
        assertFalse(BritishLocalePolicy.isBritish(null))
    }
}
