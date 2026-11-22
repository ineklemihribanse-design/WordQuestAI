package com.kelimebulmacasi.a1

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Dedicated matching import. Nothing is redirected to or written into
 * Kelime Hafızam. PDF, image, camera, AI and manual entry share one review gate.
 */
@Composable
internal fun Wq186MatchingImporter(
    ai: AiSession,
    profile: String,
    englishDefinitions: Boolean,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val store=remember(context,profile){Wq186MatchingFolders(context,profile)}
    var foldersRevision by remember { mutableIntStateOf(0) }
    val folders=remember(foldersRevision,profile){store.all().filter {
        it.englishDefinitions==englishDefinitions
    }}
    var folderId by rememberSaveable { mutableStateOf("") }
    var folderName by rememberSaveable { mutableStateOf("Yeni eşleştirme klasörüm") }
    var deleteCandidate by remember { mutableStateOf<Wq186MatchFolder?>(null) }
    var level by rememberSaveable { mutableStateOf("A1") }
    var topic by rememberSaveable { mutableStateOf("Günlük yaşam") }
    var aiCount by rememberSaveable { mutableIntStateOf(30) }
    var pages by rememberSaveable { mutableStateOf("1") }
    var english by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    var staged by remember { mutableStateOf<List<Wq175MatchPair>>(emptyList()) }
    var excluded by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var page by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var pendingCamera by remember { mutableStateOf<File?>(null) }

    fun addBatch(found:List<Wq175MatchPair>, origin:String) {
        val clean=found.filter{it.english.trim().length in 2..75}
            .distinctBy{it.english.trim().lowercase()}
            .take(150)
        staged=(staged+clean).distinctBy{it.english.trim().lowercase()}.take(300)
        excluded=emptySet();page=0
        status="$origin: ${clean.size} kelime taslak olarak alındı. Kaydetmeden önce anlamlarını doğrula."
    }
    fun importPairs(uri:Uri, isPdf:Boolean, camera:Boolean=false) {
        if(busy)return
        busy=true;status="Kelimeler okunuyor…"
        scope.launch {
            try {
                val result=scanPairs(context,uri,isPdf,pages.ifBlank{"1"},null) { _,_ -> }
                addBatch(result.map {
                    Wq175MatchPair(level,"Kişisel",it.answer,
                        if(englishDefinitions) {
                            // OCR can return Turkish translations. Never save those as
                            // supposedly English definitions without an explicit edit.
                            ""
                        } else it.clue)
                },if(isPdf)"PDF" else "Görsel")
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:SecurityException){status="Dosya izni reddedildi veya dosya korumalı."}
            catch(e:Exception){status=e.message?.take(120)?:"İçe aktarma başarısız."}
            finally {
                if(camera){pendingCamera?.delete();pendingCamera=null}
                busy=false
            }
        }
    }
    val openDocument=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if(uri!=null) {
            val pdf=runCatching{isPdf(context,uri)}.getOrDefault(false)
            importPairs(uri,pdf)
        }
    }
    val capture=rememberLauncherForActivityResult(remember{TakeScanPhoto()}) { ok ->
        val file=pendingCamera
        if(ok && file!=null && file.length()>0L)
            importPairs(scanPhotoUri(context,file),false,camera=true)
        else {
            pendingCamera?.delete();pendingCamera=null
            status="Kamera çekimi iptal edildi."
        }
    }
    fun openCamera() {
        try {
            pendingCamera=createScanPhoto(context)
            capture.launch(scanPhotoUri(context,pendingCamera!!))
        } catch(_:ActivityNotFoundException) {
            pendingCamera?.delete();pendingCamera=null
            status="Kamera uygulaması bulunamadı."
        } catch(_:Exception) {
            pendingCamera?.delete();pendingCamera=null
            status="Kamera açılamadı."
        }
    }
    fun suggest() {
        if(busy)return
        if(!ai.hasKey) {
            status="Önce Ayarlar'da yapay zekâ sağlayıcını etkinleştir."
            return
        }
        busy=true;status="Yapay zekâ kelime önerilerini hazırlıyor…"
        scope.launch {
            try {
                // The existing AI word endpoint accepts bounded requests.
                // Accumulate up to 50 unique suggestions in safe 10-word batches.
                // Do not widen a shared endpoint's constraints for one UI.
                val used=(staged+folders.flatMap{it.words})
                    .map{englishLetters(it.english)}.toMutableSet()
                val found=withContext(Dispatchers.IO) {
                    val collected=mutableListOf<WordPair>()
                    repeat((aiCount+9)/10) {
                        val desired=minOf(10,aiCount-collected.size)
                        if(desired<=0)return@repeat
                        val batch=ai.suggestWords("",topic,level,desired,used)
                        for(pair in batch) {
                            val key=englishLetters(pair.answer)
                            if(used.add(key))collected.add(pair)
                        }
                    }
                    collected.take(aiCount)
                }
                addBatch(found.map {
                    Wq175MatchPair(level,"AI",it.answer,
                        if(englishDefinitions)"" else it.clue)
                },"Yapay zekâ")
                if(englishDefinitions)
                    status="AI kelimeleri önerdi. İngilizce tanımları tek tek doğrulayıp doldur."
            } catch(cancelled:CancellationException){throw cancelled}
            catch(e:Exception){status=e.message?.take(140)?:"Yapay zekâdan sonuç alınamadı."}
            finally {busy=false}
        }
    }
    val available=staged.withIndex().filter { (index,pair) ->
        index !in excluded &&
            pair.english.trim().length in 2..75 &&
            pair.turkish.trim().length in (if(englishDefinitions) 8 else 2)..200 &&
            (!englishDefinitions || pair.turkish.trim().contains(' '))
    }.map{it.value}
    deleteCandidate?.let { folder ->
        AlertDialog(onDismissRequest={deleteCandidate=null},
            title={Text("Klasörü sil?")},
            text={Text("${folder.name} klasörü ve bu klasördeki eşleştirme kelimeleri " +
                "silinecek. Diğer klasörlerin ve Kelime Hafızam'ın verileri korunur.")},
            confirmButton={
                TextButton(onClick={
                    try {
                        if(store.remove(folder.id)){
                            foldersRevision++
                            if(folderId==folder.id){folderId="";folderName=""}
                            status="Klasör silindi."
                        }
                    }catch(_:Exception){status="Klasör silinemedi."}
                    deleteCandidate=null
                }){Text("Sil")}
            },
            dismissButton={TextButton(onClick={deleteCandidate=null}){Text("Vazgeç")}}
        )
    }
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},
        title={Text(if(englishDefinitions)"İngilizce Tanım Klasörü"
            else "Eşleştirmeye Kelime Aktar")},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=560.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("Kelimeler yalnızca Eşleştirmelerim klasöründe saklanır.",
                    fontSize=12.sp)
                if(englishDefinitions) Text(
                    "Sağdaki anlamlar İngilizce olmalı. PDF, görsel ve AI önerilerini " +
                    "İngilizce tanım olarak kontrol etmeden kaydetme.",
                    fontSize=12.sp)
                Text("Eşleştirme klasörlerim",fontSize=13.sp)
                OutlinedButton(onClick={folderId="";folderName="Yeni eşleştirme klasörüm"},
                    modifier=Modifier.fillMaxWidth()) {Text("＋ Yeni klasör")}
                folders.forEach { folder ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick={
                            folderId=folder.id
                            folderName=folder.name
                        },modifier=Modifier.weight(1f)) {
                            Text(folder.name.take(28)+" · ${folder.words.size}",
                                fontSize=11.sp)
                        }
                        TextButton(onClick={
                            folderId=folder.id
                            folderName=folder.name
                            status="Klasör adını düzenleyip kaydet."
                        }){Text("Düzenle",fontSize=11.sp)}
                        TextButton(onClick={deleteCandidate=folder}) {
                            Text("Sil",fontSize=11.sp)
                        }
                    }
                }
                OutlinedTextField(folderName,{folderName=it.take(60)},
                    label={Text("Klasör adı")},modifier=Modifier.fillMaxWidth())
                if(folderId.isNotBlank()) TextButton(onClick={
                    try {
                        if(store.rename(folderId,folderName)){
                            foldersRevision++
                            status="Klasör adı değiştirildi."
                        }else status="Klasör adı değiştirilemedi."
                    }catch(_:Exception){status="Klasör adı kaydedilemedi."}
                }) {Text("Klasör adını kaydet")}
                Text("Seviye",fontSize=13.sp)
                Wq186OptionTiles(listOf("A1","A2","B1","B2","C1","C2"),
                    setOf(level),!busy) { level=it }
                OutlinedTextField(pages,{pages=it.take(25)},
                    label={Text("PDF sayfaları (örn. 1-3, 6)")},
                    modifier=Modifier.fillMaxWidth(),singleLine=true)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick={openDocument.launch(arrayOf("application/pdf"))},
                        enabled=!busy,modifier=Modifier.weight(1f)) {Text("PDF",fontSize=12.sp)}
                    OutlinedButton(onClick={openDocument.launch(arrayOf("image/*"))},
                        enabled=!busy,modifier=Modifier.weight(1f)) {Text("Görsel",fontSize=12.sp)}
                    OutlinedButton(onClick={openCamera()},
                        enabled=!busy,modifier=Modifier.weight(1f)) {Text("Kamera",fontSize=12.sp)}
                }
                OutlinedTextField(topic,{topic=it.take(60)},
                    label={Text("AI konusu")},modifier=Modifier.fillMaxWidth())
                Wq186OptionTiles(listOf("10","20","30","50"),
                    setOf(aiCount.toString()),!busy) { aiCount=it.toInt() }
                OutlinedButton(onClick={suggest()},enabled=!busy,
                    modifier=Modifier.fillMaxWidth()) {
                    Text("Yapay zekâdan $aiCount kelime öner")
                }
                HorizontalDivider()
                Text("Elle kelime ekle",fontSize=14.sp)
                OutlinedTextField(english,{english=it.take(75)},
                    label={Text("İngilizce kelime")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(meaning,{meaning=it.take(200)},
                    label={Text(if(englishDefinitions)"English definition" else "Türkçe anlam")},
                    modifier=Modifier.fillMaxWidth())
                OutlinedButton(onClick={
                    addBatch(listOf(Wq175MatchPair(level,"Elle eklendi",english.trim(),
                        meaning.trim())),"Elle ekleme")
                    english="";meaning=""
                },enabled=english.trim().length>=2 && meaning.trim().length>=2,
                    modifier=Modifier.fillMaxWidth()) {Text("Listeye ekle")}
                Text("Önizleme · ${staged.size} kelime · kaydedilebilir: ${available.size}",
                    fontSize=12.sp)
                val last=(staged.size-1).coerceAtLeast(0)/8
                if(staged.size>8) Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(onClick={page=(page-1).coerceAtLeast(0)}){Text("Önceki")}
                    Text("${page+1}/${last+1}",fontSize=12.sp)
                    TextButton(onClick={page=(page+1).coerceAtMost(last)}){Text("Sonraki")}
                }
                staged.withIndex().drop(page*8).take(8).forEach { (index,item) ->
                    Row(Modifier.fillMaxWidth()) {
                        Checkbox(index !in excluded,onCheckedChange={
                            excluded=if(it)excluded-index else excluded+index
                        })
                        Column(Modifier.weight(1f)) {
                            OutlinedTextField(item.english,
                                { value -> staged=staged.toMutableList().also {
                                    it[index]=item.copy(english=value.take(75)) } },
                                label={Text("English")},singleLine=true,
                                modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(item.turkish,
                                { value -> staged=staged.toMutableList().also {
                                    it[index]=item.copy(turkish=value.take(200)) } },
                                label={Text(if(englishDefinitions)"English definition"
                                    else "Türkçe anlam")},
                                modifier=Modifier.fillMaxWidth())
                        }
                    }
                }
                if(status.isNotBlank()) Text(status,fontSize=12.sp)
            }
        },
        confirmButton={
            Button(onClick={
                try {
                    val selected=store.save(folderName,englishDefinitions,available,
                        folderId.ifBlank{null})
                    if(selected==null)status="Klasör kaydedilemedi. Kelimeleri ve adı kontrol et."
                    else {foldersRevision++;onDone(selected.id)}
                } catch(_:Exception) {
                    status="Kayıt yapılamadı. Cihaz depolamasını kontrol et."
                }
            },enabled=!busy && available.isNotEmpty() && folderName.isNotBlank()) {
                Text("Klasöre kaydet")
            }
        },
        dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("Kapat")}},
    )
}
