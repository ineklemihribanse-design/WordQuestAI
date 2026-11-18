package com.kelimebulmacasi.a1

import kotlin.random.Random

internal enum class VerbPool(val label: String) {
    All("Tümü"), Irregular("Düzensiz"), Regular("Düzenli"),
}

internal enum class VerbExercise(val title: String, val description: String, val icon: String) {
    Match("Eşleştirme", "Fiilin yalın hâlini 2. ve 3. hâliyle eşleştir.", "↔"),
    Gap("Boşluk doldurma", "İngilizce cümleye uygun fiil hâlini yaz.", "…"),
    Write("Yazma", "Türkçe anlamdan hareketle üç hâli de yaz.", "Aa"),
}

internal data class VerbForms(
    val type: VerbPool, val forms: List<List<String>>, val meaning: String, val blankForm: Int, val sentence: String,
) {
    val id get() = forms[0].first()
    val base get() = label(0)

    fun label(form: Int) = forms[form].joinToString(" / ")

    fun accepts(form: Int, answer: String) =
        answer.trim().replace('ı', 'i').replace('İ', 'I').lowercase() in forms[form]
}

internal fun loadVerbForms(lines: List<String>): List<VerbForms> {
    val spelling = Regex("[a-z]+")
    val verbs = lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { line ->
        val columns = line.split('\t')
        require(columns.size == 7)
        val type = when (columns[0]) {
            "D" -> VerbPool.Irregular
            "R" -> VerbPool.Regular
            else -> error("Fiil türü geçersiz")
        }
        val forms = columns.subList(1, 4).map { it.split('|') }
        require(forms.all { alternatives -> alternatives.all(spelling::matches) && alternatives.distinct() == alternatives })
        val blank = columns[5].toInt() - 1
        require(blank in 0..2 && columns[4].isNotBlank() && columns[6].split("___").size == 2)
        VerbForms(type, forms, columns[4], blank, columns[6])
    }
    require(verbs.flatMap { it.forms[0] }.let { it.size == it.toSet().size })
    require(verbs.distinctBy { it.label(1) to it.label(2) }.size == verbs.size)
    return verbs
}

internal fun verbRound(verbs: List<VerbForms>, pool: VerbPool, exercise: VerbExercise, seed: Int): List<VerbForms> {
    val random = Random(seed)
    val choices = verbs.filter { pool == VerbPool.All || it.type == pool }.shuffled(random)
    if (exercise == VerbExercise.Match) return choices.take(5)
    if (exercise == VerbExercise.Write) return choices.take(10)
    val byForm = (0..2).mapNotNull { form -> choices.firstOrNull { it.blankForm == form } }
    return (byForm + choices.filterNot(byForm::contains).take(10 - byForm.size)).shuffled(random)
}
