package com.kelimebulmacasi.a1

/**
 * Thirty additional original, explanatory YDS-style questions.
 * No copyrighted past-exam questions or placeholder answer keys.
 * Questions are added to (never replace) the existing bank.
 */
internal object Wq175YdsExpansion {
    private val easy = YdsDifficulty.Easy
    private val medium = YdsDifficulty.Medium
    private val hard = YdsDifficulty.Hard
    private val paragraph = YdsTopic.entries.firstOrNull {
        it.label.contains("Paragraf", ignoreCase = true)
    } ?: YdsTopic.Completion
    private val translation = YdsTopic.entries.firstOrNull {
        it.label.contains("Çeviri", ignoreCase = true)
    } ?: YdsTopic.Completion

    private fun q(
        id: String, topic: YdsTopic, difficulty: YdsDifficulty, prompt: String,
        correct: String, wrong1: String, wrong2: String, wrong3: String,
        explanation: String
    ) = YdsQuestion(
        id = "expanded175_$id", topic = topic, difficulty = difficulty,
        question = prompt, options = listOf(correct, wrong1, wrong2, wrong3),
        answer = 0, explanation = explanation
    )

    val questions: List<YdsQuestion> = listOf(
        // Dilbilgisi — easy, medium and hard.
        q("g01",YdsTopic.Grammar,easy,
          "By the time we arrived, the film ____.",
          "had already started","has already started","will already start","already starts",
          "İki geçmiş olaydan daha önce gerçekleşen eylem past perfect ile anlatılır: had started."),
        q("g02",YdsTopic.Grammar,easy,
          "The report must ____ before Friday.",
          "be submitted","submit","submitted","be submitting",
          "Must + be + V3 edilgen zorunluluk yapısıdır; rapor teslim edilir, teslim eden özne değildir."),
        q("g03",YdsTopic.Grammar,medium,
          "Despite ____ very tired, she completed the analysis.",
          "being","to be","be","she was",
          "Despite edatından sonra isim veya -ing yapısı gelir: despite being tired."),
        q("g04",YdsTopic.Grammar,medium,
          "The researcher, ____ findings were published last month, won an award.",
          "whose","which","whom","where",
          "Whose, araştırmacının bulgularıyla sahiplik ilişkisi kurar; findings isim öbeğini niteler."),
        q("g05",YdsTopic.Grammar,hard,
          "Only after the data had been verified ____ the team release its conclusions.",
          "did","had","was","has",
          "Only after cümle başında olduğunda ana cümlede inversion gerekir: did + özne + yalın fiil."),
        q("g06",YdsTopic.Grammar,hard,
          "Were the proposal to be approved, additional funding ____ required.",
          "would be","will have","has been","is being",
          "Were + özne + to V, ikinci tip koşulun devrik biçimidir; sonuçta would kullanılır."),

        // Cümle tamamlama.
        q("c01",YdsTopic.Completion,easy,
          "Although the weather was cold, ____.",
          "the children continued playing outside","the roads will always be closed","it had never rained before","a thermometer measures temperature",
          "Although zıtlık kurar: soğuğa rağmen çocukların dışarıda oynamaya devam etmesi tutarlıdır."),
        q("c02",YdsTopic.Completion,easy,
          "You should save your work regularly so that ____.",
          "you do not lose your progress","you forgot the password yesterday","the screen was repaired last week","the battery had already died",
          "So that bir amaç bildirir; düzenli kaydetmenin amacı ilerleme kaybetmemektir."),
        q("c03",YdsTopic.Completion,medium,
          "Unless governments invest in public transport, ____.",
          "traffic congestion is likely to worsen","traffic problems would have disappeared yesterday","all commuters had been walking","road construction has never existed",
          "Unless = if not. Yatırım yapılmazsa sıkışıklığın artması olası sonuçtur."),
        q("c04",YdsTopic.Completion,medium,
          "The study was repeated with a larger sample because ____.",
          "the researchers wanted more reliable results","the conclusion had been printed in a newspaper","every participant was given the same name","the laboratory was built in 1890",
          "Daha büyük örneklemle tekrarlamanın mantıklı gerekçesi sonuçların güvenilirliğini artırmaktır."),
        q("c05",YdsTopic.Completion,hard,
          "Had the warning signs been identified earlier, ____.",
          "the damage might have been prevented","the damage will be prevented tomorrow","the damage is prevented every year","the warning signs have no meaning",
          "Had + özne + V3 üçüncü tip koşulun devrik biçimidir; sonuç might have + V3 olur."),
        q("c06",YdsTopic.Completion,hard,
          "Notwithstanding the methodological limitations, ____.",
          "the findings offer useful preliminary evidence","all conclusions are mathematically certain","the data can never be examined","the researchers did not collect any observations",
          "Notwithstanding = despite. Sınırlamalara rağmen bulguların ön kanıt sunması makul bir çıkarımdır."),

        // Yakın anlam.
        q("s01",YdsTopic.Synonym,easy,
          "'The meeting was postponed.' cümlesine en yakın anlam hangisidir?",
          "The meeting was moved to a later time.","The meeting was cancelled permanently.","The meeting began earlier than planned.","The meeting lasted longer than expected.",
          "Postpone bir etkinliği ertelemek, daha sonraki bir zamana almak demektir; cancel iptal etmektir."),
        q("s02",YdsTopic.Synonym,easy,
          "'The task is optional.' ifadesinin anlamına en yakın seçenek nedir?",
          "You do not have to do the task.","You must finish the task immediately.","The task is impossible to complete.","The task has already been completed.",
          "Optional zorunlu olmayan anlamına gelir; must ile eşdeğer değildir."),
        q("s03",YdsTopic.Synonym,medium,
          "'The committee rejected the proposal.' cümlesine en yakın anlam hangisidir?",
          "The committee did not accept the proposal.","The committee postponed the proposal for one day.","The committee prepared a new proposal.","The committee explained the proposal to the public.",
          "Reject = kabul etmemek. Postpone, prepare ve explain aynı sonucu bildirmez."),
        q("s04",YdsTopic.Synonym,medium,
          "'The evidence remains inconclusive.' cümlesine en yakın anlam hangisidir?",
          "The evidence does not yet support a definite conclusion.","The evidence proves the theory beyond doubt.","The evidence was removed from the report.","The evidence is no longer relevant to any question.",
          "Inconclusive, kesin hükme varmaya yetmeyen kanıt demektir."),
        q("s05",YdsTopic.Synonym,hard,
          "'The policy was implemented despite considerable opposition.' cümlesinin eşdeğeri hangisidir?",
          "The policy was put into effect even though many opposed it.","The policy was withdrawn because it received broad support.","The policy was proposed only after opposition had ended.","The policy was delayed until nobody opposed it.",
          "Implement = uygulamaya koymak; despite = -e rağmen. Her iki anlam da doğru seçenekte korunur."),
        q("s06",YdsTopic.Synonym,hard,
          "'The findings call into question the prevailing assumption.' cümlesine en yakın anlam hangisidir?",
          "The findings cast doubt on the widely accepted belief.","The findings confirm that the common belief cannot be wrong.","The findings explain why no assumption has ever existed.","The findings are unrelated to any existing interpretation.",
          "Call into question bir varsayımı sorgulamak; prevailing ise yaygın kabul gören demektir."),

        // Paragraf okuma ve çıkarım.
        q("p01",paragraph,easy,
          "Mina walks to school every morning. She enjoys the short journey because she can see the trees and birds in the park. Why does Mina enjoy walking to school?",
          "She likes seeing nature along the way.","She can buy books at the park.","She wants to avoid studying.","Her school is inside the park.",
          "Paragrafta yolculuğu sevmesinin nedeni parkta ağaçları ve kuşları görebilmesidir."),
        q("p02",paragraph,easy,
          "The town library opens at nine in the morning. On Sundays, it opens two hours later. At what time does the library open on Sundays?",
          "At eleven in the morning.","At seven in the morning.","At nine in the evening.","At eight in the morning.",
          "Normal açılış 09.00; pazar günü iki saat sonra açıldığı için yanıt 11.00'dir."),
        q("p03",paragraph,medium,
          "Many cities are planting trees near busy streets. Trees can provide shade and improve the appearance of neighbourhoods, but they need regular care during dry seasons. What is a key point of the text?",
          "Urban trees have benefits but also require maintenance.","Planting trees immediately eliminates all air pollution.","City trees do not need water in summer.","Trees should be planted only far from streets.",
          "Metin hem yararları hem de özellikle kurak dönemde bakım gereksinimini vurgular."),
        q("p04",paragraph,medium,
          "Researchers compared two groups of students. Both groups studied the same material, but one group took short breaks. That group reported less fatigue, although both groups earned similar test scores. Which conclusion is supported?",
          "Short breaks may reduce fatigue without necessarily improving scores.","Short breaks always result in higher test scores.","The group without breaks did not study the material.","Students can learn only when they take breaks.",
          "Daha az yorgunluk bildirilmiş fakat sınav puanları benzer; puan artışı çıkarılamaz."),
        q("p05",paragraph,hard,
          "A coastal community introduced restrictions on fishing during breeding seasons. Initial catches fell, prompting criticism. Five years later, surveys found larger fish populations, but the report warned that changes in water temperature might also have contributed. What is the most cautious inference?",
          "Restrictions may have helped, but the population increase cannot be attributed to them alone.","Restrictions were the sole proven cause of the population increase.","Water temperature had no possible effect on fish populations.","The restrictions immediately increased catches in the first year.",
          "Rapor alternatif etkeni açıkça belirtir; tek nedenin kısıtlama olduğu sonucuna gidilemez."),
        q("p06",paragraph,hard,
          "An education programme improved attendance in several districts. However, districts volunteered to participate and also received additional transport funding. The evaluation therefore avoided claiming that the programme itself caused the entire improvement. Why was a causal claim avoided?",
          "Self-selection and extra funding could also explain improved attendance.","Attendance was not measured by the evaluation.","All districts had exactly identical circumstances.","The programme had been conducted without any participants.",
          "Gönüllü katılım ve ek ulaşım desteği karıştırıcı değişken olabilir; nedensellik kesinleşmemiştir."),

        // Çeviri — iki yön.
        q("t01",translation,easy,
          "'Regular exercise can improve sleep quality.' cümlesinin doğru Türkçe çevirisi hangisidir?",
          "Düzenli egzersiz uyku kalitesini artırabilir.","Düzenli uyku egzersiz yapmayı yasaklar.","Egzersiz yalnızca sabahları yapılmalıdır.","Uyku kalitesi egzersizle mutlaka azalır.",
          "Can improve olasılık/yeterlilik bildirir: artırabilir; kesinlik veya yasaklama içermez."),
        q("t02",translation,easy,
          "'Bu kitap geçen yıl yayımlandı.' cümlesinin İngilizce karşılığı hangisidir?",
          "This book was published last year.","This book will publish next year.","This book publishes every year.","This book is publishing yesterday.",
          "Geçen yıl tamamlanan edilgen eylem past simple passive ile aktarılır: was published."),
        q("t03",translation,medium,
          "'The results should be interpreted with caution.' cümlesinin doğru çevirisi hangisidir?",
          "Sonuçlar dikkatle yorumlanmalıdır.","Sonuçların yorumlanması yasaktır.","Sonuçlar hiçbir açıklama gerektirmez.","Sonuçlar yalnızca gelecek yıl yorumlanacak.",
          "Should be interpreted edilgen öneri/zorunluluk bildirir; with caution = dikkatle."),
        q("t04",translation,medium,
          "'Araştırma, kirliliğin son on yılda arttığını gösteriyor.' cümlesinin karşılığı hangisidir?",
          "The research shows that pollution has increased over the past decade.","The research showed that pollution would disappear next week.","The research denies that pollution ever existed.","The research shows that pollution decreased a century ago.",
          "Son on yılda bugüne uzanan değişim present perfect ile; has increased artışı anlatır."),
        q("t05",translation,hard,
          "'Unless further evidence emerges, the hypothesis will remain unverified.' cümlesinin doğru Türkçe çevirisi hangisidir?",
          "Yeni kanıtlar ortaya çıkmadıkça hipotez doğrulanmamış kalacaktır.","Yeni kanıtlar ortaya çıktığı için hipotez kesin biçimde doğrulanmıştır.","Hipotez, kanıtlar ne olursa olsun geçmişte doğrulanmıştır.","Daha fazla kanıtın var olması hipotezi otomatik olarak yanlışlar.",
          "Unless = -madıkça; will remain unverified = doğrulanmamış kalacaktır."),
        q("t06",translation,hard,
          "'Politikanın uzun vadeli etkileri henüz kesin olarak değerlendirilememiştir.' cümlesinin en doğru İngilizce karşılığı hangisidir?",
          "The long-term effects of the policy have not yet been conclusively assessed.","The policy had no long-term effects under any circumstances.","The long-term policy has already been abandoned without assessment.","The effects of the policy will certainly be positive.",
          "Have not yet been assessed present perfect passive'dir; conclusively kesin biçimde anlamını verir.")
    )
}
