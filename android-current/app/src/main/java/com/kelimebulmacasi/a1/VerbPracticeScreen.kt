package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private val formLabels = listOf("1. hâl (V1)", "2. hâl (V2)", "3. hâl (V3)")
private val stringsSaver = Saver<List<String>, ArrayList<String>>(save = { ArrayList(it) }, restore = { it.toList() })

@Composable
internal fun VerbMenu(pool: VerbPool, choose: (VerbPool) -> Unit, back: () -> Unit, start: (VerbExercise) -> Unit) {
    ScreenPage("Fiillerin 1., 2. ve 3. Hâli", "V1 yalın hâl · V2 Past Simple · V3 Past Participle", back) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (option in VerbPool.entries) {
                FilterChip(
                    selected = pool == option, onClick = { choose(option) }, label = { Text(option.label, fontFamily = Sans) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
                )
            }
        }
        for (exercise in VerbExercise.entries) {
            MenuItem(exercise.title, exercise.description, exercise.icon) { start(exercise) }
        }
    }
}

@Composable
internal fun VerbPracticeScreen(
    verbs: List<VerbForms>, pool: VerbPool, exercise: VerbExercise, seed: Int, back: () -> Unit, newRound: () -> Unit,
    preferences: WordQuestPreferences,
) {
    val round = remember(verbs, pool, exercise, seed) { verbRound(verbs, pool, exercise, seed) }
    key(exercise, pool, seed) {
        if (exercise == VerbExercise.Match) VerbMatching(round, seed, back, newRound)
        else VerbTyping(round, exercise, back, newRound, preferences)
    }
}

@Composable
private fun VerbMatching(round: List<VerbForms>, seed: Int, back: () -> Unit, newRound: () -> Unit) {
    val right = remember(round, seed) {
        val shuffled = round.shuffled(Random(seed xor 8192))
        if (shuffled == round) shuffled.drop(1) + shuffled.take(1) else shuffled
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var matched by rememberSaveable(stateSaver = stringsSaver) { mutableStateOf(emptyList<String>()) }
    var mistakes by rememberSaveable { mutableIntStateOf(0) }
    var wrong by rememberSaveable { mutableStateOf(false) }

    ScreenPage(VerbExercise.Match.title, "Soldan bir fiil seç, sağdan 2. ve 3. hâlini bul.", back) {
        BodyText("${matched.size}/${round.size} eşleşme", Forest)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                BodyText("Yalın hâl (V1)")
                for (verb in round) {
                    VerbChoice(verb.base, verb.id == selected, verb.id in matched, verb.id !in matched) {
                        selected = verb.id
                        wrong = false
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                BodyText("V2 ve V3")
                for (verb in right) {
                    VerbChoice("${verb.label(1)}\n${verb.label(2)}", false, verb.id in matched, selected != null && verb.id !in matched) {
                        if (verb.id == selected) {
                            matched = matched + verb.id
                            selected = null
                            wrong = false
                        } else {
                            mistakes++
                            wrong = true
                        }
                    }
                }
            }
        }
        if (wrong) BodyText("Bu eşleşme yanlış. Tekrar dene.", ErrorInk)
        if (matched.size == round.size) {
            BodyText(if (mistakes == 0) "Tüm eşleşmeleri ilk denemede yaptın." else "Tüm eşleşmeler tamamlandı · $mistakes yanlış deneme", Forest)
            PrimaryAction("Yeni tur", newRound)
        }
    }
}

@Composable
private fun VerbChoice(text: String, selected: Boolean, matched: Boolean, enabled: Boolean, choose: () -> Unit) {
    OutlinedButton(
        onClick = choose, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).semantics {
            if (matched) stateDescription = "Eşleşti"
            else if (selected) stateDescription = "Seçili"
        },
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Forest else White, contentColor = if (selected) White else Ink,
            disabledContainerColor = if (matched) LightLime else White, disabledContentColor = if (matched) Forest else Muted,
        ),
    ) {
        Text(text, fontFamily = Sans, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VerbTyping(round: List<VerbForms>, exercise: VerbExercise, back: () -> Unit,
    newRound: () -> Unit, preferences: WordQuestPreferences) {
    val context = LocalContext.current
    val correctSound = remember(context) { CorrectAnswerSound(context) }
    DisposableEffect(correctSound) { onDispose { correctSound.close() } }
    val focus = LocalFocusManager.current
    var question by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var answers by rememberSaveable(stateSaver = stringsSaver) { mutableStateOf(listOf("", "", "")) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var checked by rememberSaveable { mutableStateOf(false) }
    var correct by rememberSaveable { mutableStateOf(false) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    val verb = round.getOrNull(question)

    ScreenPage(exercise.title, exercise.description, back) {
        if (verb == null) {
            Text("$score / ${round.size}", fontFamily = Serif, color = Forest, fontSize = 42.sp)
            BodyText("İlk denemede doğru cevap")
            PrimaryAction("Yeni tur", newRound)
        } else {
            val fillSentence = exercise == VerbExercise.Gap
            val fields = if (fillSentence) listOf(verb.blankForm) else formLabels.indices.toList()
            val canCheck = fields.all { answers[it].isNotBlank() }

            fun checkAnswer() {
                if (!canCheck || correct || revealed) return
                correct = fields.all { verb.accepts(it, answers[it]) }
                if (correct && !attempted) score++
                attempted = true
                checked = true
                if (correct) {
                    correctSound.play(preferences.correctSoundEnabled, preferences.correctSoundVolume)
                    focus.clearFocus()
                }
            }

            BodyText("Soru ${question + 1} / ${round.size}", Forest)
            Column(
                Modifier.fillMaxWidth().background(LightLime, RoundedCornerShape(16.dp)).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(if (fillSentence) verb.sentence else verb.meaning, fontFamily = Serif, color = Ink, fontSize = 26.sp, lineHeight = 31.sp)
                if (fillSentence) BodyText(if (verb.blankForm == 0) verb.meaning else "${verb.base} · ${verb.meaning}", Forest)
            }
            for (form in fields) {
                val last = form == fields.last()
                OutlinedTextField(
                    value = answers[form],
                    onValueChange = { value ->
                        answers = answers.mapIndexed { index, answer -> if (form == index) value.take(35) else answer }
                        checked = false
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = correct || revealed,
                    label = { Text(if (fillSentence) "Eksik kelime" else formLabels[form], fontFamily = Sans) },
                    isError = checked && !verb.accepts(form, answers[form]),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false,
                        imeAction = if (last) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onDone = { checkAnswer() }),
                )
            }
            if (correct || revealed) {
                Column(
                    Modifier.fillMaxWidth().background(White, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    BodyText(if (correct) "Doğru" else "Doğru cevap", Forest)
                    BodyText(fields.joinToString(" · ") { form -> "V${form + 1} ${verb.label(form)}" }, Ink)
                }
                PrimaryAction(if (question == round.lastIndex) "Sonucu gör" else "Sonraki soru", {
                    focus.clearFocus()
                    answers = listOf("", "", "")
                    attempted = false
                    checked = false
                    correct = false
                    revealed = false
                    question++
                })
            } else {
                if (checked) BodyText("İşaretli alanları kontrol edip tekrar dene.", ErrorInk)
                PrimaryAction("Kontrol et", ::checkAnswer, canCheck)
                TextButton(onClick = { focus.clearFocus(); revealed = true; checked = false }) {
                    Text("Cevabı göster", fontFamily = Sans)
                }
            }
        }
    }
}
