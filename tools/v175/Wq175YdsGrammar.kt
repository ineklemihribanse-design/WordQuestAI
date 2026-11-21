package com.kelimebulmacasi.a1

/** Fifteen original questions: five per difficulty, with actionable Turkish explanations. */
internal object Wq175YdsGrammar {
    private val E = YdsDifficulty.Easy
    private val M = YdsDifficulty.Medium
    private val H = YdsDifficulty.Hard
    private fun q(id: String, difficulty: YdsDifficulty, prompt: String,
        correct: String, wrong1: String, wrong2: String, wrong3: String,
        explanation: String) = YdsQuestion(
        id = "g175_$id", topic = YdsTopic.Grammar, difficulty = difficulty,
        question = prompt, options = listOf(correct,wrong1,wrong2,wrong3),
        answer = 0, explanation = explanation
    )

    val questions = listOf(
        q("e01", E, "She ____ to the library every Saturday. Boşluğu tamamla.",
          "goes", "go", "going", "gone",
          "Every Saturday, geniş zaman işaretidir. She üçüncü tekil olduğu için go fiiline -es eklenir."),
        q("e02", E, "There ____ several interesting books on the shelf.",
          "are", "is", "has", "was",
          "Several books çoğuldur. Şimdiki genel durumu anlatan there are yapısı seçilir."),
        q("e03", E, "My brother is interested ____ science.",
          "in", "on", "at", "for",
          "Be interested in kalıp ifadesi bir konuya ilgi duymayı anlatır; in edatı gerekir."),
        q("e04", E, "The woman ____ lives next door is a doctor.",
          "who", "which", "where", "when",
          "İnsanları niteleyen özne görevindeki sıfat cümleciklerinde who kullanılır."),
        q("e05", E, "This documentary is ____ than the previous one.",
          "more interesting", "most interesting", "interest", "interesting as",
          "İki şey karşılaştırılırken uzun sıfatlar için more + adjective + than kalıbı kullanılır."),

        q("m01", M, "If I had known about the delay, I ____ home earlier.",
          "would have left", "will leave", "had left", "would leave",
          "Üçüncü tip koşul: If + past perfect, would have + üçüncü hâl; geçmişte gerçekleşmeyen olasılık anlatılır."),
        q("m02", M, "The documents ____ before the committee meeting yesterday.",
          "had been prepared", "have prepared", "will be preparing", "are preparing",
          "Geçmişteki toplantıdan daha önce tamamlanmış edilgen eylem past perfect passive (had been + V3) ile gösterilir."),
        q("m03", M, "He said that he ____ the assignment the day before.",
          "had completed", "will complete", "has completed", "completes",
          "Geçmiş zamanda aktarılan, daha önce bitmiş eylem için reported speech içinde past perfect kullanılır."),
        q("m04", M, "Neither the manager nor the employees ____ satisfied.",
          "are", "is", "has", "was",
          "Neither...nor yapısında fiil çoğunlukla en yakın özneyle uyum gösterir; employees çoğuldur."),
        q("m05", M, "Having ____ the data carefully, the team published the report.",
          "reviewed", "review", "reviewing", "reviews",
          "Having + past participle, ana eylemden önce tamamlanmış işi vurgular: having reviewed."),

        q("h01", H, "Not until the final interview ____ the importance of preparation.",
          "did they realise", "they realised", "did they realised", "had they realise",
          "Olumsuz zaman zarfı Not until cümle başına geldiğinde ana cümlede yardımcı fiil ile öznenin yeri değişir."),
        q("h02", H, "It is essential that every applicant ____ valid identification.",
          "provide", "provides", "provided", "providing",
          "Essential that kalıbından sonra resmî İngilizcede mandative subjunctive kullanılır; fiil yalın kalır."),
        q("h03", H, "If she had accepted the scholarship, she ____ abroad now.",
          "would be studying", "will be studying", "would have studied", "studies",
          "Karma koşul: geçmişte gerçekleşmeyen koşulun şimdiki sonucu would + be + -ing ile ifade edilir."),
        q("h04", H, "Hardly ____ the lecture begun when the fire alarm sounded.",
          "had", "has", "did", "was",
          "Hardly...when yapısında geçmişteki iki olay için past perfect inversiyon kullanılır: hardly had + özne + V3."),
        q("h05", H, "The findings ____ in the appendix require further analysis.",
          "presented", "presenting", "were presented", "presents",
          "The findings (that were) presented biçimindeki edilgen sıfat cümleciği past participle ile kısaltılır.")
    )
}
