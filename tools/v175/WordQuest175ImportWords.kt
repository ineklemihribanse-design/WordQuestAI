package com.kelimebulmacasi.a1

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun Wq175WordAddBar(onManual:()->Unit,onImport:()->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Button(onClick=onManual,modifier=Modifier.weight(1f).heightIn(min=54.dp),
            shape=RoundedCornerShape(16.dp),
            colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF5AA5DD))) {
            Text("+  Elle Ekle",fontSize=12.sp,fontWeight=FontWeight.ExtraBold)
        }
        OutlinedButton(onClick=onImport,modifier=Modifier.weight(1f).heightIn(min=54.dp),
            shape=RoundedCornerShape(16.dp)) {
            Text("PDF · Kamera · AI",fontSize=12.sp,
                color=Color(0xFF60389A),fontWeight=FontWeight.Bold)
        }
    }
}

/**
 * Uses EXISTING WordQuest OCR (PDF renderer + MLKit) and personal AI provider.
 * Nothing is written until the learner reviews and confirms editable pairs.
 */
@Composable
internal fun Wq175ImportWordsDialog(
    session:AppSession,ai:AiSession,onDismiss:()->Unit
) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var pages by remember{mutableStateOf("1")}
    var topic by remember{mutableStateOf("Günlük yaşam")}
    var level by remember{mutableStateOf("A1")}
    var pairs by remember{mutableStateOf<List<WordPair>>(emptyList())}
    val excluded=remember{mutableStateMapOf<Int,Boolean>()}
    var pendingCamera by remember{mutableStateOf<File?>(null)}

    fun stage(found:List<WordPair>,label:String) {
        val valid=found.filter { it.valid }.distinctBy { englishLetters(it.answer) }.take(150)
        pairs=valid
        excluded.clear()
        status=if(valid.isEmpty()) "Geçerli İngilizce–Türkçe çiftleri bulunamadı."
            else label+": "+valid.size+" kelime. Kaydetmeden önce gözden geçir."
    }
    fun scan(uri:Uri,document:Boolean,fromCamera:Boolean=false) {
        if(busy)return
        busy=true
        status="Belge taranıyor..."
        val selection=pages.trim().ifBlank{"1"}
        scope.launch {
            try {
                val found=scanPairs(context,uri,document,selection,null) { index,total ->
                    withContext(Dispatchers.Main) {
                        status="Taranıyor: "+index+"/"+total
                    }
                }
                stage(found,if(document)"PDF" else "Görsel")
            }catch(cancelled:CancellationException){throw cancelled}
            catch(e:SecurityException){
                status="Dosya izni verilemedi veya PDF korumalı."
            }catch(e:Exception){
                status=e.message?.take(130)?:"Dosyadan kelime okunamadı."
            }finally {
                if(fromCamera) {pendingCamera?.delete();pendingCamera=null}
                busy=false
            }
        }
    }
    val openDocument=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){uri->
        if(uri!=null) {
            val pdf=runCatching{isPdf(context,uri)}.getOrDefault(false)
            scan(uri,pdf)
        }
    }
    val capture=rememberLauncherForActivityResult(remember{TakeScanPhoto()}) {ok->
        val file=pendingCamera
        if(ok&&file!=null&&file.length()>0L)
            scan(scanPhotoUri(context,file),false,fromCamera=true)
        else {
            pendingCamera?.delete();pendingCamera=null
            status="Kamera çekimi iptal edildi veya dosya açılamadı."
        }
    }
    fun camera() {
        try {
            pendingCamera=createScanPhoto(context)
            capture.launch(scanPhotoUri(context,pendingCamera!!))
        }catch(_:ActivityNotFoundException){
            pendingCamera?.delete();pendingCamera=null
            status="Bu cihazda uygun kamera bulunamadı."
        }catch(_:Exception){
            pendingCamera?.delete();pendingCamera=null
            status="Kamera başlatılamadı."
        }
    }
    fun aiSuggest() {
        if(busy)return
        if(!ai.hasKey){
            status="Önce Ayarlar'dan yapay zekâ anahtarını etkinleştir."
            return
        }
        busy=true
        status="Yapay zekâ kelime önerilerini hazırlıyor..."
        val used=session.searchLearning("",0,5000).map{it.answer}.toSet()
        scope.launch {
            try {
                val found=ai.suggestWords("",topic,level,10,used)
                stage(found,"AI önerisi")
            }catch(cancelled:CancellationException){throw cancelled}
            catch(e:Exception){
                status="Yapay zekâ önerisi alınamadı: "+(e.message?:"Bağlantıyı kontrol et")
            }finally{busy=false}
        }
    }

    AlertDialog(onDismissRequest={if(!busy)onDismiss()},
        title={Text("Kelimeleri İçe Aktar",fontWeight=FontWeight.ExtraBold,
            color=Color(0xFF293C69))},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=520.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("PDF, fotoğraf veya AI önerileri önce burada düzenlenir. "+
                    "Onay olmadan deftere kaydedilmez.",
                    fontSize=12.sp,color=Color(0xFF667082))
                OutlinedTextField(pages,{pages=it.take(25)},
                    label={Text("PDF sayfaları · örn. 1-3, 6")},
                    singleLine=true,modifier=Modifier.fillMaxWidth(),
                    enabled=!busy)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(onClick={
                        openDocument.launch(arrayOf("application/pdf"))},
                        enabled=!busy,modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(horizontal=3.dp)) {
                        Text("PDF",fontSize=12.sp)
                    }
                    OutlinedButton(onClick={
                        openDocument.launch(arrayOf("image/*"))},
                        enabled=!busy,modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(horizontal=3.dp)) {
                        Text("Görsel",fontSize=12.sp)
                    }
                    OutlinedButton(onClick={camera()},enabled=!busy,
                        modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(horizontal=3.dp)) {
                        Text("Kamera",fontSize=12.sp)
                    }
                }
                HorizontalDivider()
                OutlinedTextField(topic,{topic=it.take(60)},
                    label={Text("AI konusu")},singleLine=true,
                    modifier=Modifier.fillMaxWidth(),enabled=!busy)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf("A1","A2","B1","B2","C1","C2").forEach{value->
                        FilterChip(selected=level==value,onClick={level=value},
                            enabled=!busy,label={Text(value,fontSize=11.sp)})
                    }
                }
                OutlinedButton(onClick={aiSuggest()},enabled=!busy,
                    modifier=Modifier.fillMaxWidth()) {
                    Text("Yapay Zekâdan 10 Kelime Öner",
                        fontWeight=FontWeight.Bold,fontSize=12.sp)
                }
                Text("AI isteği bağlı sağlayıcının ücretlendirmesine tabi olabilir.",
                    fontSize=10.sp,color=Color(0xFF73768F))
                if(busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                if(status.isNotBlank()) Text(status,fontSize=12.sp,
                    color=Color(0xFF4D6A86))
                if(pairs.isNotEmpty()) {
                    Text("Önizleme · "+pairs.size+" kayıt",
                        fontWeight=FontWeight.ExtraBold,color=Color(0xFF263D65))
                    pairs.forEachIndexed{index,pair->
                        Column(Modifier.fillMaxWidth()
                            .background(Color(0xFFF7F4FF),RoundedCornerShape(14.dp))
                            .padding(8.dp),
                            verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Checkbox(checked=excluded[index]!=true,
                                    onCheckedChange={excluded[index]=!it})
                                Text("Kelime "+(index+1),
                                    fontWeight=FontWeight.SemiBold,fontSize=12.sp)
                            }
                            OutlinedTextField(pair.answer,
                                {value->pairs=pairs.toMutableList().also {
                                    it[index]=pair.copy(answer=value.take(75))
                                }},
                                label={Text("English")},singleLine=true,
                                modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(pair.clue,
                                {value->pairs=pairs.toMutableList().also {
                                    it[index]=pair.copy(clue=value.take(140))
                                }},
                                label={Text("Türkçe")},singleLine=true,
                                modifier=Modifier.fillMaxWidth())
                        }
                    }
                    Button(onClick={
                        var saved=0
                        var skipped=0
                        for((index,pair) in pairs.withIndex()){
                            if(excluded[index]==true)continue
                            if(!pair.valid){skipped++;continue}
                            if(session.addManualLearning(pair.answer,pair.clue,""))saved++
                            else skipped++
                        }
                        pairs=emptyList()
                        excluded.clear()
                        status="Deftere "+saved+" kelime eklendi. "+skipped+
                            " geçersiz veya tekrarlanan kayıt atlandı."
                    },enabled=!busy,modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(
                            containerColor=Color(0xFF4B91C6))) {
                        Text("Seçilen Kelimeleri Kaydet")
                    }
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss,enabled=!busy){
            Text("Kapat")
        }},
        containerColor=Color.White)
}
