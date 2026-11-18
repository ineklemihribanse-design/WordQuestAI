package com.kelimebulmacasi.a1

import java.util.Locale

/** Only true English/British locales qualify. Australian and US voices are excluded. */
internal object BritishLocalePolicy {
    fun isBritish(locale: Locale?): Boolean {
        if (locale == null) return false
        val language = locale.language.lowercase(Locale.ROOT)
        val region = locale.country.uppercase(Locale.ROOT)
        return language in setOf("en", "eng") && region in setOf("GB", "UK", "GBR")
    }
}
