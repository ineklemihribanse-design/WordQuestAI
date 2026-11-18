package com.kelimebulmacasi.a1

import java.util.Locale

/** v0.9.0 original, fully offline exercises. No remote AI service is required for these. */
internal enum class PracticeKind(val title: String, val detail: String) {
    Dictation("Dinle ve yaz", "Amerikan veya Britanya sesini dinle; kelimeyi yaz"),
    Sentence("Cümle oluştur", "Karışık kelimeleri doğru sıraya yerleştir"),
    Grammar("Dilbilgisi hata avcısı", "İngilizce cümledeki hatayı düzelt"),
    Story("Hikâyeyi tamamla", "Bağlama uygun kelimeyi seç"),
    Speak("Konuşma alıştırması", "Dinle ve tekrarla; cihazın ses tanıma sonucunu karşılaştır"),
}

internal data class PracticeQuestion(
    val instruction: String,
    val answer: String,
    val options: List<String> = emptyList(),
    val explanation: String = "",
)

internal object PracticeContent {
    val sentences = listOf(
        PracticeQuestion("Her sabah okula yürürüm.", "I walk to school every morning", explanation = "Sıklık ve zaman ifadesi çoğunlukla cümlenin sonunda yer alır."),
        PracticeQuestion("Şu anda kitap okuyor.", "She is reading a book now", explanation = "Şu anda süren eylemlerde present continuous kullanılır."),
        PracticeQuestion("Dün sinemaya gittik.", "We went to the cinema yesterday", explanation = "Geçmiş zamanda go fiilinin ikinci hâli went'tir."),
        PracticeQuestion("Yarın seni arayacağım.", "I will call you tomorrow", explanation = "Will + fiilin yalın hâli gelecek zaman için kullanılabilir."),
        PracticeQuestion("Bu çanta benimkinden daha ağır.", "This bag is heavier than mine", explanation = "Karşılaştırmada -er + than kullanılır."),
        PracticeQuestion("Hiç Londra'ya gittin mi?", "Have you ever been to London", explanation = "Hayat deneyimlerinde present perfect kullanılabilir."),
        PracticeQuestion("O henüz ödevini bitirmedi.", "He has not finished his homework yet", explanation = "Yet, olumsuz present perfect cümlelerinde yaygındır."),
        PracticeQuestion("Hava güzel olursa dışarı çıkacağız.", "If the weather is nice we will go outside", explanation = "Birinci tip koşul cümlesinde if + present, will + fiil."),
        PracticeQuestion("Dün saat sekizde ders çalışıyordum.", "I was studying at eight yesterday", explanation = "Geçmişte sürmekte olan eylem past continuous ile anlatılır."),
        PracticeQuestion("İngilizceyi geliştirmek istiyorum.", "I want to improve my English", explanation = "Want fiilinden sonra to + fiil kullanılır."),
        PracticeQuestion("Her gün yeterince su içmelisin.", "You should drink enough water every day", explanation = "Should öğüt vermek için kullanılır."),
        PracticeQuestion("Bu kitap geçen yıl yazıldı.", "This book was written last year", explanation = "Passive voice: was + fiilin üçüncü hâli."),
    )

    val grammar = listOf(
        PracticeQuestion("She go to school every day.", "She goes to school every day", listOf("She goes to school every day", "She going to school every day", "She is go to school every day"), "He/she/it için simple present fiiline -s/-es gelir."),
        PracticeQuestion("I have seen him yesterday.", "I saw him yesterday", listOf("I seen him yesterday", "I saw him yesterday", "I have saw him yesterday"), "Yesterday, tamamlanmış geçmiş zaman anlatır; past simple kullanılır."),
        PracticeQuestion("There is many people here.", "There are many people here", listOf("There is much people here", "There are many people here", "There was many people here"), "People çoğuldur: there are."),
        PracticeQuestion("He don't like coffee.", "He doesn't like coffee", listOf("He doesn't likes coffee", "He not like coffee", "He doesn't like coffee"), "Üçüncü tekilde doesn't + yalın fiil."),
        PracticeQuestion("She is more taller than me.", "She is taller than me", listOf("She is taller than me", "She is more tall than me", "She taller than me"), "Taller zaten karşılaştırma biçimidir; more eklenmez."),
        PracticeQuestion("I am agree with you.", "I agree with you", listOf("I agreeing with you", "I agree with you", "I was agree with you"), "Agree bir fiildir; normal cümlede am gerektirmez."),
        PracticeQuestion("We didn't went to work.", "We didn't go to work", listOf("We didn't go to work", "We didn't gone to work", "We not went to work"), "Didn't sonrasında fiilin yalın hâli gelir."),
        PracticeQuestion("She has lived here since five years.", "She has lived here for five years", listOf("She lived here since five years", "She has lived here for five years", "She has live here since five years"), "Süre için for, başlangıç anı için since kullanılır."),
        PracticeQuestion("If it will rain, we will stay home.", "If it rains, we will stay home", listOf("If it rains, we will stay home", "If it rain, we will stay home", "If it would rain, we will stay home"), "Birinci tip koşulun if bölümünde present simple kullanılır."),
        PracticeQuestion("I look forward to meet you.", "I look forward to meeting you", listOf("I look forward to meet you", "I look forward to meeting you", "I look forward meeting you"), "Look forward to sonrasında -ing gelir."),
        PracticeQuestion("He can to swim.", "He can swim", listOf("He can swimming", "He can to swim", "He can swim"), "Can sonrasında fiilin yalın hâli gelir."),
        PracticeQuestion("The news are good.", "The news is good", listOf("The news is good", "The news be good", "The news are good"), "News tekil fiille kullanılır."),
    )

    val story = listOf(
        PracticeQuestion("Ali missed the bus, so he had to ____ to school.", "walk", listOf("walk", "sleep", "cook", "swim"), "Otobüsü kaçırdığı için okula yürümek zorunda kaldı."),
        PracticeQuestion("It was raining. Zeynep opened her ____.", "umbrella", listOf("window", "umbrella", "book", "bag"), "Yağmurda kullanılan nesne umbrella (şemsiye)."),
        PracticeQuestion("The room was dark, so I turned on the ____.", "light", listOf("light", "fridge", "chair", "door"), "Karanlık odada ışık açılır."),
        PracticeQuestion("Ece was tired. She decided to go to ____ early.", "bed", listOf("work", "market", "bed", "school"), "Yorgun olduğu için erken yatmaya karar verdi."),
        PracticeQuestion("Can was thirsty, so he drank a glass of ____.", "water", listOf("water", "bread", "paper", "sand"), "Thirsty = susamış."),
        PracticeQuestion("The train was late. We waited at the ____.", "station", listOf("station", "kitchen", "garden", "beach"), "Train station = tren istasyonu."),
        PracticeQuestion("My phone battery was low. I needed a ____.", "charger", listOf("camera", "pencil", "charger", "ticket"), "Low battery = düşük pil; charger = şarj aleti."),
        PracticeQuestion("She studied hard and passed the ____.", "exam", listOf("exam", "window", "meal", "weather"), "Pass an exam = sınavı geçmek."),
        PracticeQuestion("We bought fresh apples at the ____.", "market", listOf("forest", "market", "hospital", "library"), "Fresh apples = taze elmalar."),
        PracticeQuestion("The child was cold. His mother gave him a ____.", "coat", listOf("coat", "glass", "plate", "clock"), "Cold = üşümüş; coat = mont."),
        PracticeQuestion("It was her birthday, and her friends brought a ____.", "cake", listOf("cake", "train", "key", "lamp"), "Birthday cake = doğum günü pastası."),
        PracticeQuestion("He couldn't find the address, so he checked the ____.", "map", listOf("map", "spoon", "shoes", "pillow"), "Address bulmak için map (harita) kullanılır."),
    )

    /** Normalization accepts capitalization and surface punctuation, but not missing words. */
    fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace('’', '\'').replace(Regex("[^a-z0-9' ]+"), " ")
        .replace(Regex("\\s+"), " ").trim()

    fun correct(input: String, answer: String): Boolean = normalize(input) == normalize(answer)

    fun mixedTokens(question: PracticeQuestion, seed: Int): List<Pair<Int, String>> {
        val tokens = question.answer.split(" ").mapIndexed { index, s -> index to s }
        val random = kotlin.random.Random(seed)
        // Ensure the exercise isn't already solved after shuffling.
        var mixed = tokens.shuffled(random)
        if (mixed == tokens && tokens.size > 1) mixed = mixed.reversed()
        return mixed
    }

    fun dailyGoal(minutes: Int): Int = when (minutes) {
        15 -> 5
        30 -> 10
        60 -> 20
        else -> error("Unknown daily goal: $minutes")
    }
}
