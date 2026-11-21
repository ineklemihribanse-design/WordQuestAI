package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Never take the first N records of the corpus or use a fixed seed on launch.
 * A bounded, profile-specific recent list survives process restarts and
 * minimizes repeats across YDS exams and Daily Plan practice sessions.
 * No user answers, account identity or actual question text are persisted.
 */
internal object Wq176QuestionRotation {
    /**
     * Draw an unpredictable topic order without repeatedly cycling through
     * the same six categories. Each category occurs once per bag. Persist the
     * remaining bag per profile so leaving and reopening Daily Plan does not
     * reset its sequence; never repeat the previous category at a bag boundary.
     */
    fun nextTopic(context: Context, profileKey: String, categories: List<YdsTopic>): YdsTopic {
        val unique = categories.distinct()
        require(unique.isNotEmpty()) { "At least one practice topic required" }
        val prefs=context.getSharedPreferences("wq176_daily_topics",Context.MODE_PRIVATE)
        val suffix=profileKey.hashCode().toString()
        val bagKey="bag.$suffix"
        val lastKey="last.$suffix"
        val stored=prefs.getString(bagKey,"").orEmpty()
        val current=stored.split(",").mapNotNull { item ->
            unique.firstOrNull { it.name==item }
        }.distinct()
        val pending=if(current.isNotEmpty())current else {
            val shuffled=unique.shuffled().toMutableList()
            val previous=prefs.getString(lastKey,null)
            if(shuffled.size>1 && shuffled.first().name==previous) {
                val swapIndex=shuffled.indexOfFirst { it.name!=previous }
                val tmp=shuffled[0]
                shuffled[0]=shuffled[swapIndex]
                shuffled[swapIndex]=tmp
            }
            shuffled
        }
        val selected=pending.first()
        prefs.edit()
            .putString(bagKey,pending.drop(1).joinToString(",") { it.name })
            .putString(lastKey,selected.name)
            .apply()
        return selected
    }

    fun pick(
        context: Context, profileKey: String, pool: List<YdsQuestion>, count: Int
    ): List<YdsQuestion> {
        if (count <= 0 || pool.isEmpty()) return emptyList()
        val prefs=context.getSharedPreferences("wq176_question_rotation",Context.MODE_PRIVATE)
        val key="recent."+profileKey.hashCode()
        val recent=prefs.getString(key,"").orEmpty().split(",").filter(String::isNotBlank)
        val recentSet=recent.toHashSet()
        val unique=pool.distinctBy { it.id }
        val first=unique.filter { it.id !in recentSet }.shuffled()
        val second=unique.filter { it.id in recentSet }.shuffled()
        val result=(first+second).take(count)
        // Do not retain more history than is useful for small topic pools.
        val keep=(recent+result.map { it.id }).distinct().takeLast(650)
        prefs.edit().putString(key,keep.joinToString(",")).apply()
        return result
    }
}

@Composable
internal fun Wq176DailyPracticeEntry(profileKey: String) {
    val context=LocalContext.current
    val bank=YdsContent.questions
    var selectedLevel by remember(profileKey) { mutableStateOf("A1") }
    val wordDecks=remember(profileKey) { mutableMapOf<String,List<YdsQuestion>>() }
    fun next(level:String=selectedLevel): YdsQuestion? {
        val categories=listOf(
            YdsTopic.Vocabulary, YdsTopic.Grammar, YdsTopic.Translation,
            YdsTopic.Completion, YdsTopic.Paragraph, YdsTopic.Connectors
        )
        val topic=Wq176QuestionRotation.nextTopic(context,profileKey,categories)
        val matched=if(topic==YdsTopic.Vocabulary) {
            wordDecks.getOrPut(level) { Wq176VocabularyPractice.questions(context,level) }
        } else {
            bank.filter { it.topic==topic }
        }
        val selected=Wq176QuestionRotation.pick(
            context,profileKey,matched.ifEmpty { bank },1).firstOrNull()
        return selected
    }
    var question by remember(profileKey) { mutableStateOf(next()) }
    var answer by remember(profileKey) { mutableStateOf<Int?>(null) }
    val q=question
    val optionOrder=remember(q?.id) { q?.options?.indices?.shuffled().orEmpty() }
    val shape=RoundedCornerShape(22.dp)
    Surface(
        modifier=Modifier.fillMaxWidth().padding(vertical=7.dp),
        shape=shape,color=Color(0xFFF2F9FF),
        border=BorderStroke(1.dp,Color(0xFFC8DEED))
    ) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Günlük karışık çalışma · YDS + Kelime",
                color=Color(0xFF194E66),fontWeight=FontWeight.ExtraBold,fontSize=17.sp
            )
            Text(
                "Kelimeler gerçek seviye kütüphanenden, diğer sorular YDS havuzundan seçilir. Yakın zamanda çözdüklerin öncelik almaz.",
                color=Color(0xFF596F78),fontSize=12.sp
            )
            Row(
                modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)
            ) {
                listOf("A1","A2","B1","B2","C1","C2").forEach { level ->
                    OutlinedButton(
                        onClick={ selectedLevel=level; question=next(level); answer=null },
                        shape=RoundedCornerShape(12.dp),
                        contentPadding=androidx.compose.foundation.layout.PaddingValues(horizontal=10.dp,vertical=4.dp),
                        border=BorderStroke(
                            if(level==selectedLevel)2.dp else 1.dp,
                            if(level==selectedLevel)Color(0xFF169F89) else Color(0xFFC8D5DE)
                        ),
                        colors=ButtonDefaults.outlinedButtonColors(
                            containerColor=if(level==selectedLevel)Color(0xFFDEF7EA) else Color.White
                        )
                    ) {
                        Text(level,fontSize=12.sp,fontWeight=FontWeight.Bold,
                            color=Color(0xFF28555A))
                    }
                }
            }
            if(q!=null) {
                Text(q.topic.label+" · "+q.difficulty.label,
                    color=Color(0xFF297D83),fontSize=12.sp)
                Text(q.question,color=Color(0xFF1A3041),
                    fontWeight=FontWeight.SemiBold,fontSize=15.sp)
                optionOrder.forEach { index ->
                    val chosen=answer==index
                    val isCorrect=answer!=null && index==q.answer
                    val bg=when {
                        isCorrect -> Color(0xFFDDF5E7)
                        chosen -> Color(0xFFFFEAE5)
                        else -> Color.White
                    }
                    Surface(
                        onClick={
                            if(answer==null) {
                                answer=index
                                DailyPlanStore(context,profileKey)
                                    .mark(
                                        if(q.id.startsWith("wq176_word_"))
                                            PracticeKind.Sentence else PracticeKind.Grammar,
                                        q.id
                                    )
                            }
                        },
                        enabled=answer==null,shape=RoundedCornerShape(13.dp),
                        color=bg,
                        border=BorderStroke(1.dp,Color(0xFFD6E2E5)),
                        modifier=Modifier.fillMaxWidth()
                    ) {
                        Text(
                            ('A'.code+optionOrder.indexOf(index)).toChar().toString()+
                                ") "+q.options[index],
                            modifier=Modifier.padding(12.dp),
                            color=Color(0xFF163F48),fontSize=13.sp
                        )
                    }
                }
                if(answer!=null) {
                    Text(
                        if(answer==q.answer) "Doğru! "+q.explanation
                        else "Doğru cevap: "+q.options[q.answer]+"\n"+q.explanation,
                        fontSize=12.sp,color=Color(0xFF285947)
                    )
                    Button(
                        onClick={question=next(); answer=null},
                        modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1C756C))
                    ) { Text("Yeni soru getir") }
                }
            } else {
                Text("Şu anda kullanılabilir soru bulunamadı.")
            }
        }
    }
}
