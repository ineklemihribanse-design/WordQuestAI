package com.kelimebulmacasi.a1

/** Original, offline questions. These are not copied from past ÖSYM examinations. */
internal enum class YdsTopic(val label: String) {
    Vocabulary("Akademik kelime"), Connectors("Bağlaçlar"), Phrasals("Phrasal verbs"),
    Paragraph("Paragraf"), Translation("Çeviri"), Completion("Cümle tamamlama"), Synonym("Yakın anlam")
}

internal enum class YdsDifficulty(val label: String) {
    Easy("Kolay"), Medium("Orta"), Hard("Zor")
}

internal data class YdsQuestion(
    val id: String,
    val topic: YdsTopic,
    val difficulty: YdsDifficulty,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String,
    val focusWord: String = "",
) {
    init {
        require(id.isNotBlank() && question.isNotBlank() && explanation.isNotBlank())
        require(options.size == 4 && options.distinct().size == 4 && answer in 0..3)
    }
}

internal object YdsContent {
    private fun q(id: String, topic: YdsTopic, level: YdsDifficulty, question: String,
                  correct: String, wrong1: String, wrong2: String, wrong3: String,
                  explanation: String, focus: String = "") = YdsQuestion(
        id, topic, level, question, listOf(correct, wrong1, wrong2, wrong3), 0, explanation, focus)

    private val V = YdsTopic.Vocabulary
    private val C = YdsTopic.Connectors
    private val P = YdsTopic.Phrasals
    private val R = YdsTopic.Paragraph
    private val T = YdsTopic.Translation
    private val F = YdsTopic.Completion
    private val S = YdsTopic.Synonym
    private val E = YdsDifficulty.Easy
    private val M = YdsDifficulty.Medium
    private val H = YdsDifficulty.Hard

    val questions: List<YdsQuestion> = listOf(
        q("v01", V, E, "The new policy will ____ many students. Boşluğu tamamla.", "benefit", "damage", "hide", "forget", "Benefit = yarar sağlamak. Politikanın öğrencileri olumlu etkilemesi bekleniyor.", "benefit"),
        q("v02", V, E, "The scientist collected ____ from the experiment.", "evidence", "furniture", "traffic", "clothing", "Evidence = kanıt. Deney sonucu elde edilen bulguları ifade eder.", "evidence"),
        q("v03", V, E, "A ____ increase in prices means a large increase.", "significant", "tiny", "temporary", "silent", "Significant = kayda değer, önemli; burada artışın büyüklüğünü niteler.", "significant"),
        q("v04", V, M, "Many species are threatened by the ____ of their habitats.", "destruction", "instruction", "selection", "celebration", "Destruction = tahribat; habitatların yok edilmesi birçok türü tehdit eder.", "destruction"),
        q("v05", V, M, "The report attempts to ____ the main causes of the problem.", "identify", "postpone", "conceal", "consume", "Identify = belirlemek, tespit etmek. Nedenler incelenerek saptanır.", "identify"),
        q("v06", V, M, "It is difficult to ____ whether the treatment is effective.", "determine", "decorate", "announce", "replace", "Determine = saptamak/karar vermek; etkinlik değerlendirmesinde kullanılır.", "determine"),
        q("v07", V, H, "The committee called for a more ____ assessment of the evidence.", "rigorous", "casual", "vague", "careless", "Rigorous = titiz ve sistematik. Bilimsel değerlendirme için uygundur.", "rigorous"),
        q("v08", V, H, "The proposed reform may ____ existing social inequalities.", "exacerbate", "alleviate", "conceal", "simplify", "Exacerbate = daha da kötüleştirmek; eşitsizlikleri artırma riski anlatılır.", "exacerbate"),
        q("v09", V, H, "The data do not ____ the researchers' initial hypothesis.", "substantiate", "undermine", "distort", "anticipate", "Substantiate = kanıtla desteklemek. Olumsuz cümlede hipotezin doğrulanmadığı söylenir.", "substantiate"),

        q("c01", C, E, "She studied hard, ____ she passed the exam.", "so", "although", "unless", "whereas", "So = bu yüzden. İlk cümle neden, ikincisi sonuçtur.", "so"),
        q("c02", C, E, "____ it was raining, they went for a walk.", "Although", "Because", "Unless", "Therefore", "Although = -e rağmen; yağmurla yürüyüş arasındaki karşıtlığı gösterir.", "although"),
        q("c03", C, E, "We stayed home ____ the roads were icy.", "because", "despite", "whereas", "however", "Because + cümle nedeni belirtir. Despite sonrasında isim/-ing gerekir.", "because"),
        q("c04", C, M, "The results were promising; ____, further research is needed.", "nevertheless", "therefore", "for example", "similarly", "Nevertheless = bununla birlikte; olumlu sonuca rağmen daha çok araştırma gerekir.", "nevertheless"),
        q("c05", C, M, "____ the heavy traffic, we arrived on time.", "Despite", "Although", "Because", "Unless", "Despite + isim öbeği kullanılır: despite the heavy traffic.", "despite"),
        q("c06", C, M, "The company reduced costs ____ increasing efficiency.", "by", "although", "unless", "since", "By + -ing = bir şeyi yaparak; verimliliği artırarak maliyet azaltıldı.", "by"),
        q("c07", C, H, "____ the sample size was small, the findings should be interpreted cautiously.", "Given that", "In case of", "As if", "Even so", "Given that = göz önüne alındığında; küçük örneklem temkinli yorum gerektirir.", "given that"),
        q("c08", C, H, "The policy would be effective ____ it were implemented consistently.", "provided that", "irrespective of", "in contrast to", "in spite of", "Provided that = koşuluyla. İkinci koşul gerçekleşirse etkililik söz konusudur.", "provided that"),
        q("c09", C, H, "The project proceeded, ____ substantial budget constraints.", "notwithstanding", "owing to", "consequently", "inasmuch as", "Notwithstanding = -e karşın. Bütçe kısıtına rağmen proje sürdü.", "notwithstanding"),

        q("p01", P, E, "Please ____ your shoes before entering the house.", "take off", "turn up", "give in", "get over", "Take off = çıkarmak (ayakkabı/giysi).", "take off"),
        q("p02", P, E, "Can you ____ this word in the dictionary?", "look up", "put away", "break down", "come across", "Look up = sözlükte arayıp bulmak.", "look up"),
        q("p03", P, E, "Don't ____; you can solve this problem.", "give up", "set off", "take after", "bring about", "Give up = pes etmek, vazgeçmek. Olumsuz emirle Don't give up şeklinde kullanılır.", "give up"),
        q("p04", P, M, "The meeting was ____ until next Monday.", "put off", "brought up", "carried out", "looked after", "Put off = ertelemek. Meeting was put off = toplantı ertelendi.", "put off"),
        q("p05", P, M, "Researchers will ____ a series of experiments.", "carry out", "run into", "turn down", "take over", "Carry out = yürütmek/gerçekleştirmek; deneylerle sık kullanılır.", "carry out"),
        q("p06", P, M, "The teacher asked us to ____ an alternative explanation.", "come up with", "look down on", "get away with", "put up with", "Come up with = fikir veya çözüm üretmek.", "come up with"),
        q("p07", P, H, "The team could not ____ the unexpected complications.", "account for", "look forward to", "run out of", "fall back on", "Account for = açıklamak/hesabını vermek; beklenmedik sonuçları açıklayamadılar.", "account for"),
        q("p08", P, H, "The revised findings ____ the earlier conclusion.", "cast doubt on", "make up for", "get along with", "catch up with", "Cast doubt on = doğruluğuna kuşku düşürmek.", "cast doubt on"),
        q("p09", P, H, "The government aims to ____ a long-term energy strategy.", "draw up", "take in", "get by", "hold on", "Draw up = plan/belge hazırlamak; strategy ile yaygın eşleşmedir.", "draw up"),

        q("r01", R, E, "Passage: 'Trees absorb carbon dioxide and release oxygen. They also provide shelter for birds.' What is the main idea?", "Trees support the environment.", "Trees use up all oxygen.", "Birds avoid trees.", "Carbon dioxide is harmless.", "İki cümlede ağaçların çevreye yararları sıralanıyor; ana fikir ilk şıktır."),
        q("r02", R, E, "Passage: 'Lena practised English for fifteen minutes each day. After three months, she understood short articles.' What helped Lena?", "Regular daily practice", "An expensive course", "Watching no videos", "Avoiding reading", "Metinde her gün on beş dakika pratik yaptığı açıkça söyleniyor."),
        q("r03", R, E, "Passage: 'The library closes at six on weekdays, but at four on Saturdays.' When does it close on Saturday?", "At four", "At six", "At eight", "At noon", "But at four on Saturdays ifadesi doğrudan cevabı verir."),
        q("r04", R, M, "Passage: 'Public transport can reduce traffic, but its success depends on reliable schedules and good coverage.' What can be inferred?", "Reliable services are essential.", "All buses are free.", "Traffic cannot be reduced.", "Schedules are unnecessary.", "Depends on reliable schedules ifadesi hizmet güvenilirliğinin önemini belirtir."),
        q("r05", R, M, "Passage: 'The researchers found an association between sleep and memory, but the study cannot prove causation.' Which statement is accurate?", "The result does not establish cause and effect.", "Sleep definitely causes better memory.", "No relationship was observed.", "The study involved no memory test.", "Association ilişkiyi, causation neden-sonucu anlatır; metin nedenselliği kanıtlamıyor."),
        q("r06", R, M, "Passage: 'Many towns have expanded cycle lanes. However, some residents say the routes do not connect key destinations.' What is the criticism?", "The routes are insufficiently connected.", "Cycling is prohibited.", "The lanes are too narrow everywhere.", "There is no public transport.", "Do not connect key destinations ifadesi bağlantı eksikliğine işaret eder."),
        q("r07", R, H, "Passage: 'Although the technology promises lower emissions, its widespread adoption hinges on infrastructure investment and regulatory clarity.' What is implied?", "Benefits alone do not ensure adoption.", "The technology has no benefits.", "Regulation has already been abolished.", "Investment is irrelevant.", "Hinges on = -e bağlıdır. Yarar sağlayabilmesi yeterli değil, altyapı ve düzenleme de gerekir."),
        q("r08", R, H, "Passage: 'The review noted that most evidence came from small observational studies, limiting the generalisability of the conclusions.' What is the principal limitation?", "The findings may not apply broadly.", "All studies were randomised.", "The sample was too diverse.", "No study was observed.", "Generalisability = sonuçların geniş nüfusa uygulanabilirliği. Küçük gözlemsel çalışmalar bunu sınırlar."),
        q("r09", R, H, "Passage: 'While automation may eliminate certain tasks, the report argues that it can also create demand for new skills.' What is the report's stance?", "Automation may both displace and create work.", "Automation only destroys jobs.", "New skills will be useless.", "Tasks can never be automated.", "While ile karşıtlık kuruluyor: bazı görevler yok olurken yeni becerilere talep oluşabilir."),

        q("t01", T, E, "'İnsanlar her gün suya ihtiyaç duyar.' cümlesinin doğru çevirisi hangisidir?", "People need water every day.", "People needed water yesterday.", "Water needs people every day.", "People never drink water.", "Need + nesne kullanılır; every day geniş zamanı destekler."),
        q("t02", T, E, "'The museum is closed on Mondays.' Türkçeye çevir.", "Müze pazartesi günleri kapalıdır.", "Müze pazartesi günleri açıktır.", "Müze yalnızca pazartesi açılır.", "Müze salı günleri kapalıdır.", "Closed = kapalı; on Mondays = pazartesi günleri."),
        q("t03", T, E, "'They are waiting for the train.' Türkçeye çevir.", "Treni bekliyorlar.", "Trene bindiler.", "Treni kaçıracaklar.", "Trenleri sayıyorlar.", "Present continuous: are waiting = şu anda bekliyorlar."),
        q("t04", T, M, "'Bu yöntem, enerji tüketimini azaltmak için geliştirilmiştir.' cümlesini çevir.", "This method was developed to reduce energy consumption.", "This method has increased energy consumption.", "This method develops energy for consumption.", "Energy consumption reduced this method.", "Was developed = geliştirildi (edilgen), to reduce = azaltmak amacıyla."),
        q("t05", T, M, "'If the results are reliable, the research may be extended.' Türkçeye çevir.", "Sonuçlar güvenilir olursa araştırma genişletilebilir.", "Sonuçlar güvenilir olmadığı için araştırma bitti.", "Araştırma sonuçları her zaman güvenilirdir.", "Araştırmanın uzatılması yasaktır.", "If koşul, may be extended ise olasılık bildiren edilgen yapıdır."),
        q("t06", T, M, "'Hükûmet, ulaşımın daha erişilebilir olmasını hedefliyor.' cümlesini çevir.", "The government aims to make transport more accessible.", "The government has banned accessible transport.", "Transport aims to replace the government.", "The government finds transport less reliable.", "Aim to + fiil hedeflemektir; more accessible = daha erişilebilir."),
        q("t07", T, H, "'Elde edilen bulgular, önceki varsayımları sorgulamayı gerektiriyor.' çevir.", "The findings call for a reassessment of earlier assumptions.", "The findings prove every earlier assumption.", "Earlier assumptions prevent any new findings.", "The findings have no relevance to assumptions.", "Call for = gerektirmek; reassessment = yeniden değerlendirme."),
        q("t08", T, H, "'The proposal is unlikely to succeed unless sufficient funding is secured.' Türkçeye çevir.", "Yeterli kaynak sağlanmadıkça önerinin başarılı olması pek olası değildir.", "Öneri kaynak olmadan kesinlikle başarılı olur.", "Önerinin başarıya ulaşması zaten kesindir.", "Öneri yeterli kaynak sağlandığı için başarısızdır.", "Unless = -medikçe; unlikely = olası değil; secured = güvence altına alınırsa."),
        q("t09", T, H, "'Artan talebe karşın, mevcut kapasite sınırlı kalmaktadır.' cümlesini çevir.", "Despite increasing demand, existing capacity remains limited.", "Because demand has fallen, capacity is limitless.", "Existing capacity is increasing faster than demand.", "Demand and capacity have both disappeared.", "Despite + isim öbeği; remains limited = sınırlı kalır."),

        q("f01", F, E, "She ____ to the library every Friday.", "goes", "go", "going", "gone", "She üçüncü tekildir; simple present için goes.", "goes"),
        q("f02", F, E, "If it rains, we ____ at home.", "will stay", "stayed", "stays", "had stayed", "Birinci tip koşul: if + geniş zaman, will + fiil.", "stay"),
        q("f03", F, E, "You should ____ more carefully.", "read", "reads", "reading", "to read", "Should modal fiilinden sonra yalın fiil gelir.", "read"),
        q("f04", F, M, "Had I known about the delay, I ____ earlier.", "would have left", "will leave", "had leave", "leave", "Devrik üçüncü tip koşul: Had I known... would have + V3.", "leave"),
        q("f05", F, M, "The more consistently you practise, ____ your progress will be.", "the faster", "more fast", "fastest", "the most fast", "The more ..., the faster ... paralel karşılaştırma kalıbıdır.", "the faster"),
        q("f06", F, M, "The results ____ before the report was published.", "had been checked", "have check", "checking", "was checking", "İki geçmiş eylemden önce gerçekleşen edilgen: past perfect passive (had been + V3).", "checked"),
        q("f07", F, H, "Not until the final review ____ the researchers detect the error.", "did", "had", "were", "have", "Cümle başındaki olumsuz sınırlayıcı ifade devrik yapı gerektirir: did + özne + fiil.", "did"),
        q("f08", F, H, "The guidelines require that each participant ____ informed consent.", "give", "gives", "gave", "giving", "Require that + özne + yalın fiil, resmî İngilizcede mandative subjunctive yapısıdır.", "give"),
        q("f09", F, H, "By the time the trial ends, researchers ____ data for two years.", "will have been collecting", "are collect", "collected", "had collecting", "Gelecekte belirli bir zamana kadar sürecek eylem: future perfect continuous.", "collect"),

        q("s01", S, E, "'Important' sözcüğünün en yakın anlamı hangisidir?", "significant", "useless", "ordinary", "distant", "Significant = önemli/kayda değer. Important ile yakın anlamlıdır.", "significant"),
        q("s02", S, E, "'Begin' sözcüğünün yakın anlamı hangisidir?", "start", "finish", "refuse", "avoid", "Begin = start = başlamak.", "start"),
        q("s03", S, E, "'Rapid' sözcüğünün yakın anlamı hangisidir?", "fast", "silent", "late", "rare", "Rapid = hızlı; fast temel eş anlamdır.", "rapid"),
        q("s04", S, M, "'The findings were inconclusive.' cümlesine en yakın anlam hangisidir?", "The results did not allow a definite conclusion.", "The results proved the theory beyond doubt.", "The research was never conducted.", "The researchers agreed on every detail.", "Inconclusive = kesin sonuca götürmeyen.", "inconclusive"),
        q("s05", S, M, "'He is reluctant to accept the offer.' ifadesine en yakın anlam hangisidir?", "He is unwilling to accept the offer.", "He eagerly accepts the offer.", "He has already refused the offer.", "He has forgotten the offer.", "Reluctant = isteksiz; unwilling aynı duyguya karşılık gelir.", "reluctant"),
        q("s06", S, M, "'The study highlights the need for further investigation.' yakın anlamı hangisidir?", "The study emphasises that more research is needed.", "The study forbids future research.", "The study states that research is unnecessary.", "The study hides all its findings.", "Highlights = vurgular; further investigation = ek araştırma.", "highlight"),
        q("s07", S, H, "'The evidence lends credence to the hypothesis.' yakın anlamı hangisidir?", "The evidence makes the hypothesis more plausible.", "The evidence completely disproves the hypothesis.", "The evidence is irrelevant to any hypothesis.", "The evidence establishes absolute certainty.", "Lend credence to = inanılırlığını/güvenilirliğini artırmak; kesin kanıtlamak değildir.", "credence"),
        q("s08", S, H, "'The intervention yielded marginal improvements.' yakın anlamı hangisidir?", "The intervention produced only slight gains.", "The intervention caused substantial gains.", "The intervention eliminated all improvements.", "The intervention was never implemented.", "Marginal = sınırlı/küçük; yielded = ortaya çıkardı.", "marginal"),
        q("s09", S, H, "'The policy has far-reaching implications.' yakın anlamı hangisidir?", "The policy may have extensive consequences.", "The policy will affect nothing.", "The policy has already been cancelled.", "The policy applies to a single minor issue.", "Far-reaching = geniş kapsamlı; implications = etkiler/sonuçlar.", "implications"),
    ) + YdsExtraContent.questions

    init {
        require(questions.map { it.id }.distinct().size == questions.size)
        require(questions.size >= 60)
        for (topic in YdsTopic.entries) for (difficulty in YdsDifficulty.entries) {
            require(questions.count { it.topic == topic && it.difficulty == difficulty } >= 3)
        }
    }

    fun pool(topic: YdsTopic?, difficulty: YdsDifficulty?): List<YdsQuestion> = questions.filter {
        (topic == null || it.topic == topic) && (difficulty == null || it.difficulty == difficulty)
    }

    /** Stable seed allows tests and replay while changing choice positions. */
    fun choices(question: YdsQuestion): List<Pair<Int, String>> =
        question.options.mapIndexed { index, option -> index to option }
            .shuffled(kotlin.random.Random(question.id.hashCode()))
}
