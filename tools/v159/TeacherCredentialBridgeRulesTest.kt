package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class TeacherCredentialBridgeRulesTest {
    @Test fun supportsSelectedSavedProvidersAndRejectsDisabledOne() {
        assertEquals(AiProvider.OpenAI, TeacherCredentialBridgeRules.selected("OpenAI"))
        assertEquals(AiProvider.Gemini, TeacherCredentialBridgeRules.selected("Gemini"))
        assertEquals(AiProvider.Gemini, TeacherCredentialBridgeRules.selected("Claude"))
        assertEquals(AiProvider.Gemini, TeacherCredentialBridgeRules.selected(null))
    }

    @Test fun neverUsesSecretFromDifferentProvider() {
        assertEquals("", TeacherCredentialBridgeRules.key("OpenAI", AiProvider.Gemini, "secret"))
        assertEquals("", TeacherCredentialBridgeRules.key("Gemini", AiProvider.OpenAI, "secret"))
        assertEquals("saved-value", TeacherCredentialBridgeRules.key("Gemini", AiProvider.Gemini, "saved-value"))
        assertEquals("", TeacherCredentialBridgeRules.key(null, AiProvider.Gemini, "saved-value"))
        assertEquals("", TeacherCredentialBridgeRules.key("Claude", AiProvider.Claude, "secret"))
    }

    @Test fun invalidStoredModelFallsBackToProviderDefault() {
        assertEquals(AiProvider.OpenAI.defaultModel, TeacherCredentialBridgeRules.model("bad / model", AiProvider.OpenAI))
        assertEquals("gpt-4.1-mini", TeacherCredentialBridgeRules.model("gpt-4.1-mini", AiProvider.OpenAI))
    }
}
