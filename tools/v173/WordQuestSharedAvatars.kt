package com.kelimebulmacasi.a1

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The catalog refers ONLY to the ten meshes supplied in the conversation.
 *  Rendered thumbnails are previews of the actual CC-BY models, NOT animated GLBs.
 *  Restricted or unverified licenses must not be distributed in the APK.
 */
internal data class WordQuestSharedAvatar(
    val id:String,val name:String,val sourceFile:String,
    val previewIndex:Int?,val licensedForBuild:Boolean
)
internal object WordQuestAvatarStore {
    val all:List<WordQuestSharedAvatar> = listOf(
        WordQuestSharedAvatar("astra","Astra","astronaut_by_jungle_jim.glb",0,true),
        WordQuestSharedAvatar("piko","Piko","le_pink_mech_by_jungle_jim.glb",1,true),
        WordQuestSharedAvatar("nobi","Nobi","cyberpunk_robot.glb",2,true),
        WordQuestSharedAvatar("tera","Tera","sci-fi_humanoid_robot.glb",3,true),
        WordQuestSharedAvatar("lopi","Lopi","rabbit-animal-3d-model-free.zip",null,false),
        WordQuestSharedAvatar("tomba","Tomba","midnight-hippo.zip",null,false),
        WordQuestSharedAvatar("foksi","Foksi","ultimate_animal_pack_100_animals__50_off.glb",null,false),
        WordQuestSharedAvatar("moka","Moka","ultimate_animal_pack_100_animals__50_off.glb",null,false),
        WordQuestSharedAvatar("pera","Pera","ultimate_animal_pack_100_animals__50_off.glb",null,false),
        WordQuestSharedAvatar("gigi","Gigi","ultimate_animal_pack_100_animals__50_off.glb",null,false)
    )
    /** Source attribution for CC-BY preview images derived from the uploaded GLBs. */
    val attribution:Map<String,Pair<String,String>> = mapOf(
        "astra" to ("Jungle Jim" to "https://sketchfab.com/3d-models/astronaut-by-jungle-jim-c775a977606a4e4e990afe783c6bc845"),
        "piko" to ("Jungle Jim" to "https://sketchfab.com/3d-models/le-pink-mech-by-jungle-jim-215dacbebf094c0292b31aeaf7b968ee"),
        "nobi" to ("Josh_NC" to "https://sketchfab.com/3d-models/cyberpunk-robot-79369b97f0d74fd1a5c4fe824040cbce"),
        "tera" to ("yur1_val" to "https://sketchfab.com/3d-models/sci-fi-humanoid-robot-12ad786ecde246e5854a61fd4f67ed49")
    )
    private val ids=all.map{it.id}.toSet()
    fun valid(id:String?):Boolean = id!=null && id in ids
    fun model(id:String?):WordQuestSharedAvatar =
        all.firstOrNull{it.id==id}?:all[1]

    fun hasSelection(context:Context,profile:String):Boolean =
        context.getSharedPreferences("wq_shared_avatar_173",Context.MODE_PRIVATE)
            .contains("avatar_$profile")

    /** Do not overwrite the original avatar preference before a new selection. */
    fun synchronizeOnlineForActiveProfile(context:Context,profile:String) {
        if(!hasSelection(context,profile)) return
        val id=read(context,profile)
        context.getSharedPreferences("wordquest_arena_mascot",Context.MODE_PRIVATE)
            .edit().putString("kind",id).apply()
    }

    fun read(context:Context,profile:String):String {
        val pref=context.getSharedPreferences("wq_shared_avatar_173",Context.MODE_PRIVATE)
        val current=pref.getString("avatar_$profile",null)
        return if(current!=null && valid(current) &&
            WordQuestPrivateAvatarAssets.installed(context,current))current else "piko"
    }
    fun save(context:Context,profile:String,id:String) {
        val choice=model(id)
        require(choice.id==id && WordQuestPrivateAvatarAssets.installed(context,id)) {
            "First import your purchased avatar ZIP on this device."
        }
        context.getSharedPreferences("wq_shared_avatar_173",Context.MODE_PRIVATE)
            .edit().putString("avatar_$profile",id).apply()
        // Preserve the online room's existing avatar preference contract.
        context.getSharedPreferences("wordquest_arena_mascot",Context.MODE_PRIVATE)
            .edit().putString("kind",id).apply()
    }
}

/**
 * Shared preference listener refreshes the avatar immediately when switching
 * between Arena and Word Memory in the same running app.
 */
@Composable
internal fun rememberWordQuestAvatar(profile:String):MutableState<String> {
    val context=LocalContext.current
    val state=remember(context,profile) {
        mutableStateOf(WordQuestAvatarStore.read(context,profile))
    }
    DisposableEffect(context,profile) {
        val prefs=context.getSharedPreferences("wq_shared_avatar_173",Context.MODE_PRIVATE)
        val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,key ->
            if(key=="avatar_$profile") {
                state.value=WordQuestAvatarStore.read(context,profile)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // Re-read after registering to avoid a race with navigation.
        state.value=WordQuestAvatarStore.read(context,profile)
        onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    return state
}

/** Preview from the user's exact GLB files, 4 CC-BY models only.
 * Not a replacement for the required bone rig and real GLB animation stage.
 */
@Composable
internal fun WordQuestAvatarPortrait(id:String,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val spec=WordQuestAvatarStore.model(id)
    val preview:Bitmap?=remember(context) {
        runCatching{
            context.assets.open("avatars/shared/wq173_verified_previews.webp").use{
                BitmapFactory.decodeStream(it)
            }
        }.getOrNull()
    }
    val tile=remember(preview,spec.id) {
        if(preview==null || spec.previewIndex==null ||
            preview.width<128 || preview.height<128) null
        else runCatching{
            val n=spec.previewIndex
            Bitmap.createBitmap(preview,(n%2)*64,(n/2)*64,64,64)
        }.getOrNull()
    }
    if(tile!=null) {
        Image(tile.asImageBitmap(),contentDescription=spec.name+" model önizlemesi",
            contentScale=ContentScale.Fit,
            modifier=modifier.clip(RoundedCornerShape(15.dp)))
    } else {
        Box(modifier.clip(RoundedCornerShape(15.dp))
            .background(Color(0xFFF2EAFF)),
            contentAlignment=Alignment.Center) {
            Text(spec.name.take(1),fontSize=23.sp,
                fontWeight=FontWeight.Bold,color=Color(0xFF7541A9))
        }
    }
}

/** The same selector is used by Arena and Word Memory. */
@Composable
internal fun WordQuestSharedAvatarPicker(
    selected:String,onSelect:(String)->Unit,onDismiss:()->Unit
) {
    val uriHandler=LocalUriHandler.current
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var imports by remember{mutableIntStateOf(WordQuestPrivateAvatarAssets.count(context))}
    var importing by remember{mutableStateOf(false)}
    var feedback by remember{mutableStateOf("")}
    val picker=rememberLauncherForActivityResult(
        contract=ActivityResultContracts.OpenDocument()
    ) { uri ->
        if(uri!=null && !importing) {
            importing=true
            feedback=""
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        WordQuestPrivateAvatarAssets.importZip(context,uri)
                    }
                }.onSuccess { result ->
                    imports=result.total
                    feedback=result.newCount.toString() + " model yüklendi. Toplam: " + imports + "/10"
                }.onFailure { e ->
                    feedback="Model içe aktarılamadı: " + (e.message?:"Dosya okunamadı")
                }
                importing=false
            }
        }
    }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Avatarını Seç",color=Color(0xFF302056),fontWeight=FontWeight.Bold)},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=490.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Seçimin Arena ve Kelime Hafızam'da aynı olacaktır.",
                    fontSize=12.sp,color=Color(0xFF746889))
                Text("Telefonundaki gerçek modeller: " + imports + "/10",
                    fontWeight=FontWeight.Bold,color=Color(0xFF60448F),fontSize=12.sp)
                OutlinedButton(
                    onClick={picker.launch(arrayOf("application/zip","application/octet-stream"))},
                    enabled=!importing,modifier=Modifier.fillMaxWidth()
                ) {
                    Text(if(importing)"Paket denetleniyor..." else
                        "3D Avatar Paketini Telefonumdan Ekle")
                }
                if(feedback.isNotBlank()) Text(feedback,
                    color=Color(0xFF76587E),fontSize=11.sp)
                WordQuestAvatarStore.all.forEach { avatar ->
                    val picked=avatar.id==selected
                    val active=WordQuestPrivateAvatarAssets.installed(context,avatar.id)
                    Row(Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if(picked)Color(0xFFE6F8DE) else Color(0xFFF7F2FC))
                        .border(if(picked)2.dp else 1.dp,
                            if(picked)Color(0xFF72BB36) else Color(0xFFE7D8F4),
                            RoundedCornerShape(16.dp))
                        .clickable(enabled=active) {
                            onSelect(avatar.id);onDismiss()
                        }.padding(8.dp),
                        horizontalArrangement=Arrangement.spacedBy(10.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        WordQuestAvatarPortrait(avatar.id,Modifier.size(54.dp))
                        Column(Modifier.weight(1f)) {
                            Text(avatar.name,fontSize=15.sp,
                                fontWeight=FontWeight.Bold,color=Color(0xFF302056))
                            Text(if(active)"3D model yüklü · Özel kullanım"
                                else "3D paketi bu telefona henüz yüklenmedi",
                                fontSize=10.sp,color=Color(0xFF786D87),
                                maxLines=2)
                        }
                        if(picked) Text("✓",color=Color(0xFF299447),
                            fontWeight=FontWeight.Bold)
                    }
                }
                if(WordQuestPrivateAvatarAssets.installed(context,selected)) {
                    WordQuestAvatarStage3D(selected,
                        Modifier.fillMaxWidth().height(155.dp),motion="select")
                }
                WordQuestAvatarStore.attribution[selected]?.let { (author,url) ->
                    Text("Model: $author · CC BY 4.0 · Önizleme uyarlanmıştır.",
                        color=Color(0xFF60537B),fontSize=11.sp)
                    TextButton(onClick={uriHandler.openUri(url)}) {
                        Text("Orijinal model ve üretici",fontSize=12.sp)
                    }
                    TextButton(onClick={
                        uriHandler.openUri("https://creativecommons.org/licenses/by/4.0/")
                    }) {
                        Text("Creative Commons lisansı",fontSize=12.sp)
                    }
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Kapat")}},
        containerColor=Color.White
    )
}
