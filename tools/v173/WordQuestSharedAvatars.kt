package com.kelimebulmacasi.a1

import android.content.Context
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
    private val ids=all.map{it.id}.toSet()
    fun valid(id:String?):Boolean = id!=null && id in ids
    fun model(id:String?):WordQuestSharedAvatar =
        all.firstOrNull{it.id==id}?:all[1]

    fun read(context:Context,profile:String):String {
        val pref=context.getSharedPreferences("wq_shared_avatar_173",Context.MODE_PRIVATE)
        val current=pref.getString("avatar_$profile",null)
        return if(current!=null && model(current).licensedForBuild)current else "piko"
    }
    fun save(context:Context,profile:String,id:String) {
        val choice=model(id)
        require(choice.id==id && choice.licensedForBuild) {
            "Avatar license or source preview is not verified for publication."
        }
        context.getSharedPreferences("wq_shared_avatar_173",Context.MODE_PRIVATE)
            .edit().putString("avatar_$profile",id).apply()
        // Preserve the online room's existing avatar preference contract.
        context.getSharedPreferences("wordquest_arena_mascot",Context.MODE_PRIVATE)
            .edit().putString("kind",id).apply()
    }
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
            preview.width<224 || preview.height<224) null
        else runCatching{
            val n=spec.previewIndex
            Bitmap.createBitmap(preview,(n%2)*112,(n/2)*112,112,112)
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
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Avatarını Seç",color=Color(0xFF302056),fontWeight=FontWeight.Bold)},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=490.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Seçimin Arena ve Kelime Hafızam'da aynı olacaktır.",
                    fontSize=12.sp,color=Color(0xFF746889))
                WordQuestAvatarStore.all.forEach { avatar ->
                    val picked=avatar.id==selected
                    val active=avatar.licensedForBuild
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
                            Text(if(active)"Gerçek model önizlemesi · Animasyon hazırlanıyor"
                                else "Lisans doğrulaması bekleniyor",
                                fontSize=10.sp,color=Color(0xFF786D87),
                                maxLines=2)
                        }
                        if(picked) Text("✓",color=Color(0xFF299447),
                            fontWeight=FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Kapat")}},
        containerColor=Color.White
    )
}
