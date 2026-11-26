package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Multi-level room editor, with real synchronized settings and pinned actions. */
@Composable
internal fun Wq132RoomSettings(match:PremiumOnlineMatch,client:PremiumOnlineRoom,packs:PremiumArenaContent,
    profileKey:String,onDismiss:()->Unit) {
    var mode by remember(match.code,match.configRevision){mutableStateOf(match.mode)}
    var pool by remember(match.code,match.configRevision){mutableStateOf(client.package6Rules.levels)}
    var topic by remember(match.code,match.configRevision){mutableStateOf(match.topic)}
    var size by remember(match.code,match.configRevision){mutableIntStateOf(match.size)}
    var seconds by remember(match.code,match.configRevision){mutableIntStateOf(match.seconds)}
    var hints by remember(match.code,match.configRevision){mutableIntStateOf(client.package6Rules.hints)}
    var retries by remember(match.code,match.configRevision){mutableIntStateOf(client.package6Rules.retries)}
    val config=LocalConfiguration.current
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var generating by remember{mutableStateOf(false)}
    val topics=remember(pool,mode){(listOf(PremiumArenaRules.allTopics)+pool.flatMap{packs.topics(it,false)}).distinct()}
    val sizes=remember(pool,topic,mode){if(mode=="quiz")listOf(5,10,12,15,20).filter{it>=pool.size} else PremiumArenaRules.allowedSizes}
    var issue by remember{mutableStateOf("")}
    LaunchedEffect(topics,sizes){if(topic !in topics)topic=PremiumArenaRules.allTopics;if(size !in sizes&&sizes.isNotEmpty())size=sizes.first()}
    MaterialTheme(colorScheme=Wq122Scheme) {
        AlertDialog(onDismissRequest=onDismiss,containerColor=Color(0xFF251347),
            title={Text("Yarışma Ayarları",color=Wq122Gold,fontWeight=FontWeight.Black)},
            text={Column(Modifier.heightIn(max=(config.screenHeightDp*.62f).dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("Ayarlar kaydedilince iki oyuncu da yeniden hazır olmalı.",color=Wq122Ink)
                Text("Oyun türü",color=Wq122Gold,fontWeight=FontWeight.Bold)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("duel" to "Düello","team" to "Ortak","quiz" to "5 Şıklı").forEach{(v,t)->Wq122Choice(t,mode==v,Modifier.weight(1f)){mode=v}}}
                Text("İngilizce seviyeleri",color=Wq122Gold,fontWeight=FontWeight.Bold)
                Wq186ArenaPresetStore.LEVELS.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    row.forEach{l->Wq122Choice(l,l in pool,Modifier.weight(1f)){if(l !in pool)pool=pool+l else if(pool.size>1)pool=pool-l}}
                }}
                Text("Konu",color=Wq122Gold,fontWeight=FontWeight.Bold)
                topics.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){row.forEach{t->Wq122Choice(t,t==topic,Modifier.weight(1f)){topic=t}};if(row.size==1)Spacer(Modifier.weight(1f))}}
                Text(if(mode=="quiz")"Soru sayısı" else "Kelime sayısı",color=Wq122Gold,fontWeight=FontWeight.Bold)
                sizes.chunked(4).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){row.forEach{n->Wq122Choice(n.toString(),n==size,Modifier.weight(1f)){size=n}}}}
                Text("Süre",color=Wq122Gold,fontWeight=FontWeight.Bold)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){PremiumArenaRules.allowedTimes.forEach{n->Wq122Choice(if(n==0)"Süresiz" else "$n sn",n==seconds,Modifier.weight(1f)){seconds=n}}}
                Text("İpucu hakkı",color=Wq122Gold,fontWeight=FontWeight.Bold)
                listOf(0,3,5,8,12,20).chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){row.forEach{n->Wq122Choice(n.toString(),n==hints,Modifier.weight(1f)){hints=n}}}}
                Text("Tekrar hakkı",color=Wq122Gold,fontWeight=FontWeight.Bold)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){(0..3).forEach{n->Wq122Choice(n.toString(),n==retries,Modifier.weight(1f)){retries=n}}}
                if(generating)Text("Ortak oyun hazırlanıyor…",color=Wq122Ink)
                if(issue.isNotBlank())Text(issue,color=Color(0xFFFFB4B4))
            }},
            confirmButton={TextButton(enabled=size in sizes&&pool.isNotEmpty()&&!client.busy&&!generating,onClick={
                val preset=Wq186MatchPreset(Wq186ArenaPresetStore.LEVELS.filter{it in pool},hints,retries)
                val requestedMode=mode;val requestedTopic=topic;val requestedSize=size
                val requestedSeconds=if(mode=="team")0 else seconds
                generating=true;issue=""
                scope.launch{
                    try{
                        val result=Wq132WebEngine.generate(context,requestedMode,preset.levels,requestedTopic,requestedSize,
                            requestedSeconds,preset.hints,preset.retries,java.security.SecureRandom().nextInt())
                        if(client.match?.code==match.code&&client.match?.configRevision==match.configRevision){
                            client.updateLobbySettings(requestedMode,preset.levels.first(),requestedTopic,requestedSize,requestedSeconds,
                                result.snapshotId,preset,result.snapshot)
                            onDismiss()
                        }
                    }catch(e:kotlinx.coroutines.CancellationException){throw e}
                    catch(e:Exception){issue="Oyun hazırlanamadı: "+(e.message?:"Başka ayar seç.").take(180)}
                    finally{generating=false}
                }
            }){Text("Kaydet",color=Wq122Gold,fontWeight=FontWeight.Bold)}},
            dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç",color=Wq122Ink)}})
    }
}
