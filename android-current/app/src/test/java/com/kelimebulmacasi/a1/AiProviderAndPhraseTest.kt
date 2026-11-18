package com.kelimebulmacasi.a1

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AiProviderAndPhraseTest {
    @Test fun phraseSpaceIsPreservedForGeneratedWordsAndScannedLists() {
        assertEquals("give up", preserveEnglishPhraseSpacing("giveup"))
        assertEquals("look after", preserveEnglishPhraseSpacing("lookafter"))
        assertEquals("come up with", preserveEnglishPhraseSpacing("comeupwith"))
        assertEquals("homework", preserveEnglishPhraseSpacing("homework"))
        assertEquals("give up", preserveEnglishPhraseSpacing("give  up"))
        assertTrue(isEnglishAnswer(preserveEnglishPhraseSpacing("giveup")))
        val fromPhoto = pastedPairs("giveup - vazgeçmek\nhomework - ev ödevi")
        assertEquals("GIVE UP", fromPhoto[0].answer)
        assertTrue(fromPhoto[0].review)
        assertEquals("HOMEWORK", fromPhoto[1].answer)
    }

    @Test fun generatedPhrasalIsNotMergedDuringParsing() {
        val content = JSONObject().put("pairs", JSONArray().put(JSONObject()
            .put("answer", "giveup").put("clue", "Vazgeçmek")))
        val geminiShape = JSONObject().put("candidates", JSONArray().put(JSONObject()
            .put("content", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",content.toString()))))))
        val parsed = readOnlineWords(geminiShape.toString(), 1, emptySet())
        assertEquals("GIVE UP", parsed.single().answer)
    }

    @Test fun providerConversionKeepsConversationRoles() {
        val request = conversationRequest(emptyList(), "Hello", ConversationLanguage.English, "B1")
        val openAi = providerRequestBody(AiProvider.OpenAI, request)
        assertEquals("system", openAi.getJSONArray("messages").getJSONObject(0).getString("role"))
        assertEquals("user", openAi.getJSONArray("messages").getJSONObject(1).getString("role"))
        val claude = providerRequestBody(AiProvider.Claude, request)
        assertEquals("user", claude.getJSONArray("messages").getJSONObject(0).getString("role"))
        assertTrue(claude.getString("system").contains("Turkish-speaking"))
    }

    @Test fun responsesFromOpenAiAndClaudeMatchExistingConversationParser() {
        val openAi = """{"choices":[{"message":{"content":"Merhaba!"}}]}"""
        val a = normalizeProviderResponse(AiProvider.OpenAI, openAi)
        assertEquals("Merhaba!",readConversationReply(a,1,"selam",ConversationLanguage.Turkish).answer)
        val claude = """{"content":[{"type":"text","text":"Nasıl gidiyor?"}]}"""
        val b = normalizeProviderResponse(AiProvider.Claude, claude)
        assertEquals("Nasıl gidiyor?",readConversationReply(b,2,"selam",ConversationLanguage.Turkish).answer)
    }

    @Test fun keysAndModelsAreValidatedSeparately() {
        assertTrue(aiModelPattern.matches("gemini-3.1-flash-lite"))
        assertFalse(aiModelPattern.matches("../model"))
        assertTrue(validatePersonalApiKey(AiProvider.OpenAI,"sk-example-personal-key-123456").startsWith("sk-"))
        assertThrows(WordRequestException::class.java) {
            validatePersonalApiKey(AiProvider.Gemini,"sk-example-personal-key-123456")
        }
    }

    @Test fun arenaAndYdsHaveAdditionalContent() {
        for (level in listOf("A1","A2","B1","B2","C1","C2")) {
            assertTrue(level in PremiumArenaRules.allowedLevels)
            assertTrue(PremiumArenaRules.matchesLevel(level, level))
        }
        assertTrue(YdsContent.questions.size >= 100)
        assertEquals(YdsContent.questions.size, YdsContent.questions.map { it.id }.toSet().size)
    }
}
