package com.kelimebulmacasi.a1

/**
 * Original practice prompts keyed to the broad pedagogical themes and grammar
 * topics in More to Read I/II and Test Attack. No book passages are copied.
 * Keep the legacy prompts as well: the new sets are additive.
 */
internal object Wq177DailyOriginals {
    val sentences:List<PracticeQuestion> = listOf(
        PracticeQuestion("Eğitim hakkında anlamlı bir İngilizce cümle kur","Regular reading improves our vocabulary."),
        PracticeQuestion("Bilimsel çalışmalarda iş birliğini anlat","Researchers share their findings with colleagues."),
        PracticeQuestion("Günlük bir alışkanlığı ifade et","My sister studies English every evening."),
        PracticeQuestion("Yenilenebilir enerji hakkında cümle kur","Solar panels can reduce household costs."),
        PracticeQuestion("Bir müzenin açılışını anlat","The museum opens at nine on weekdays."),
        PracticeQuestion("Kitap okuma alışkanlığını anlat","Children learn new words through stories."),
        PracticeQuestion("Bir öğrencinin planını anlat","The student prepared carefully for the exam."),
        PracticeQuestion("Teknolojinin kullanımını anlat","Technology helps people communicate across distances."),
        PracticeQuestion("Çevre korumayla ilgili bir öneri yaz","We should recycle paper and plastic."),
        PracticeQuestion("Bir seyahatin sonuçlarını anlat","The train arrived later than expected."),
        PracticeQuestion("Sağlıklı yaşamla ilgili bir cümle kur","Daily exercise supports physical and mental health."),
        PracticeQuestion("Bir araştırmacının gözlemini aktar","The scientist recorded the results accurately."),
        PracticeQuestion("Kent yaşamını karşılaştır","Public transport is cheaper than driving."),
        PracticeQuestion("Kültürel miras hakkında cümle kur","Ancient buildings reveal clues about history."),
        PracticeQuestion("Ders çalışma yöntemini anlat","Students remember more when they review regularly."),
        PracticeQuestion("Hava koşuluna bağlı bir plan kur","We will visit the park if it stops raining."),
        PracticeQuestion("Dil öğrenme hedefi kur","Learning another language creates new opportunities."),
        PracticeQuestion("Toplumsal iş birliği hakkında cümle kur","Volunteers helped rebuild the community library.")
    )
    val grammar:List<PracticeQuestion> = listOf(
        PracticeQuestion("She go to the library every morning.","She goes to the library every morning."),
        PracticeQuestion("The students was preparing for the exam.","The students were preparing for the exam."),
        PracticeQuestion("If it will rain, we will stay inside.","If it rains, we will stay inside."),
        PracticeQuestion("The report has written yesterday.","The report was written yesterday."),
        PracticeQuestion("He suggested to visit the museum.","He suggested visiting the museum."),
        PracticeQuestion("The scientist which discovered the error explained it.","The scientist who discovered the error explained it."),
        PracticeQuestion("She has lived here since five years.","She has lived here for five years."),
        PracticeQuestion("There is fewer information in this document.","There is less information in this document."),
        PracticeQuestion("The professor asked where did I study.","The professor asked where I studied."),
        PracticeQuestion("Despite of the delay, the train arrived.","Despite the delay, the train arrived."),
        PracticeQuestion("This method is more better than the old one.","This method is better than the old one."),
        PracticeQuestion("He must to finish the assignment tonight.","He must finish the assignment tonight."),
        PracticeQuestion("The data was collected by researchers.","The data were collected by researchers."),
        PracticeQuestion("She is interested on studying astronomy.","She is interested in studying astronomy."),
        PracticeQuestion("By the time we arrived, they have left.","By the time we arrived, they had left."),
        PracticeQuestion("The meeting will held next Monday.","The meeting will be held next Monday."),
        PracticeQuestion("I wish I know the answer now.","I wish I knew the answer now."),
        PracticeQuestion("Neither the teacher or the students were ready.","Neither the teacher nor the students were ready."),
        PracticeQuestion("She made me to rewrite the paragraph.","She made me rewrite the paragraph."),
        PracticeQuestion("Hardly had we entered than the alarm rang.","Hardly had we entered when the alarm rang.")
    )
    val story:List<PracticeQuestion> = listOf(
        PracticeQuestion("The explorer lost his compass and had to ____ the stars.","follow"),
        PracticeQuestion("The student forgot her notebook and asked to ____ one.","borrow"),
        PracticeQuestion("The volunteers worked together to ____ the river.","clean"),
        PracticeQuestion("The researcher checked the figures to ____ mistakes.","avoid"),
        PracticeQuestion("The museum guide began to ____ the exhibition.","explain"),
        PracticeQuestion("The cyclist wore a helmet to ____ his head.","protect"),
        PracticeQuestion("The librarian helped the children ____ a story.","choose"),
        PracticeQuestion("The scientist had to ____ the results again.","verify"),
        PracticeQuestion("The hikers stopped to ____ some water.","drink"),
        PracticeQuestion("The teacher asked students to ____ their ideas.","share"),
        PracticeQuestion("The engineer designed a bridge to ____ the valley.","cross"),
        PracticeQuestion("The gardener planted seeds and waited for them to ____.","grow"),
        PracticeQuestion("The visitor opened the map to ____ the station.","find"),
        PracticeQuestion("The journalist asked questions to ____ the facts.","confirm"),
        PracticeQuestion("The friends planned to ____ the historical castle.","visit"),
        PracticeQuestion("The doctor advised her patient to ____ regularly.","exercise"),
        PracticeQuestion("The photographer waited for the sun to ____.","rise"),
        PracticeQuestion("The child closed the window to ____ warm.","stay"),
        PracticeQuestion("The team collected samples to ____ the lake.","study"),
        PracticeQuestion("The musician practised daily to ____ her skills.","improve"),
        PracticeQuestion("The customer read the label to ____ the ingredients.","check"),
        PracticeQuestion("The student reread the text to ____ the meaning.","understand"),
        PracticeQuestion("The family brought umbrellas because it might ____.","rain"),
        PracticeQuestion("The travellers spoke slowly to ____ directions.","ask")
    )
    fun total(kind:PracticeKind):Int = when(kind) {
        PracticeKind.Sentence -> PracticeContent.sentences.size + sentences.size
        PracticeKind.Grammar -> PracticeContent.grammar.size + grammar.size
        PracticeKind.Story -> PracticeContent.story.size + story.size
        else -> 20
    }
}
