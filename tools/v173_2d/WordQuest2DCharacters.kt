package com.kelimebulmacasi.a1

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/** Ten original WordQuest 2D characters with distinct standard/victory poses. */
internal object Wq2DMascots {
    val ids=listOf("astra","piko","nobi","tera","lopi",
        "tomba","foksi","moka","pera","gigi")
    val labels=listOf("Astra","Piko","Nobi","Tera","Lopi",
        "Tomba","Foksi","Moka","Pera","Gigi")
    val colors=listOf(
        Color(0xFFCF98EF),Color(0xFFFFA7D2),Color(0xFF83D5FF),
        Color(0xFFA88AF2),Color(0xFFFFDFEB),Color(0xFFC4A2EB),
        Color(0xFFFFBA80),Color(0xFFFFD1A1),Color(0xFFBBEBC1),
        Color(0xFFAFE0F4)
    )
    fun valid(id:String?):Boolean=id!=null && id in ids
    fun label(id:String):String=labels.getOrElse(ids.indexOf(id)){"Piko"}
    private fun prefs(context:Context)=
        context.getSharedPreferences("wq_2d_selected_173",Context.MODE_PRIVATE)
    fun selected(context:Context,profile:String):String {
        val saved=prefs(context).getString("user_"+profile,null)
        return if(valid(saved))saved!! else "piko"
    }
    fun choose(context:Context,profile:String,id:String) {
        require(valid(id))
        prefs(context).edit().putString("user_"+profile,id).apply()
        context.getSharedPreferences("wordquest_arena_mascot",Context.MODE_PRIVATE)
            .edit().putString("kind",id).apply()
    }
    private fun directory(context:Context):File =
        File(context.filesDir,"wordquest_2d_v173").apply{mkdirs()}
    private fun file(context:Context,id:String,pose:String):File =
        File(directory(context),id+"_"+pose+".webp")
    fun available(context:Context):Boolean=ids.all { id ->
        file(context,id,"idle").isFile && file(context,id,"victory").isFile
    }
    fun picture(context:Context,id:String,victory:Boolean):Bitmap? {
        val safe=if(valid(id))id else "piko"
        val pose=if(victory)"victory" else "idle"
        val local=file(context,safe,pose)
        val imported=if(local.isFile)runCatching {
            BitmapFactory.decodeFile(local.absolutePath)
        }.getOrNull() else null
        if(imported!=null)return imported
        return runCatching {
            context.assets.open("avatars/wordquest_2d/"+safe+"_"+pose+".webp").use {
                BitmapFactory.decodeStream(it)
            }
        }.getOrNull()
    }
    /** Validate and privately install twenty 2D art files. No external CDN. */
    fun importArt(context:Context,uri:Uri):Int {
        val root=directory(context)
        val required=ids.flatMap{id->
            listOf(id+"_idle.webp",id+"_victory.webp")
        }.toSet()
        val staged=mutableListOf<Pair<File,File>>()
        val seen=mutableSetOf<String>()
        try {
            val stream=context.contentResolver.openInputStream(uri)
                ?:error("Seçilen ZIP okunamıyor")
            stream.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while(true) {
                        val entry=zip.nextEntry?:break
                        val name=entry.name.substringAfterLast('/')
                        if(!entry.isDirectory && name in required) {
                            require(seen.add(name)){"Tekrarlanan görsel: "+name}
                            require(!entry.name.contains("..")){"Geçersiz ZIP yolu"}
                            val tmp=File.createTempFile("wq2d_","_import",root)
                            staged+=tmp to File(root,name)
                            var count=0
                            FileOutputStream(tmp).buffered().use { out ->
                                val buffer=ByteArray(32768)
                                while(true) {
                                    val n=zip.read(buffer)
                                    if(n<0)break
                                    count+=n
                                    require(count<=1_000_000){"Görsel çok büyük: "+name}
                                    out.write(buffer,0,n)
                                }
                            }
                            val options=BitmapFactory.Options().apply {
                                inJustDecodeBounds=true
                            }
                            BitmapFactory.decodeFile(tmp.absolutePath,options)
                            require(options.outWidth in 160..1024 &&
                                options.outHeight in 160..1024) {
                                "Görsel bozuk: "+name
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
            require(seen==required){"WordQuest 20 görsellik 2D paketini seç"}
            staged.forEach{(tmp,target)->
                require(tmp.renameTo(target)){"Görsel kaydedilemedi: "+target.name}
            }
            val pref=prefs(context)
            pref.edit().putInt("art_revision",
                pref.getInt("art_revision",0)+1).apply()
            return seen.size
        }finally{
            staged.forEach{(tmp,_)->if(tmp.exists())tmp.delete()}
        }
    }
}

@Composable
internal fun rememberWq2DSelection(profile:String):MutableState<String> {
    val context=LocalContext.current
    val state=remember(context,profile){
        mutableStateOf(Wq2DMascots.selected(context,profile))
    }
    DisposableEffect(context,profile) {
        val p=context.getSharedPreferences("wq_2d_selected_173",Context.MODE_PRIVATE)
        val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,key->
            if(key=="user_"+profile)state.value=Wq2DMascots.selected(context,profile)
        }
        p.registerOnSharedPreferenceChangeListener(listener)
        state.value=Wq2DMascots.selected(context,profile)
        onDispose{p.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    return state
}

@Composable
internal fun Wq2DPortrait(
    id:String,modifier:Modifier=Modifier,victory:Boolean=false
) {
    val context=LocalContext.current
    val prefs=remember(context){
        context.getSharedPreferences("wq_2d_selected_173",Context.MODE_PRIVATE)
    }
    var revision by remember(prefs){mutableIntStateOf(prefs.getInt("art_revision",0))}
    DisposableEffect(prefs){
        val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,key->
            if(key=="art_revision")revision=prefs.getInt("art_revision",0)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    val bitmap=remember(context,id,victory,revision) {
        Wq2DMascots.picture(context,id,victory)
    }
    val scale by animateFloatAsState(
        targetValue=if(victory)1.06f else 1f,label="wq2d_victory")
    val color=Wq2DMascots.colors.getOrElse(Wq2DMascots.ids.indexOf(id)){
        Color(0xFFB998F0)
    }
    Box(modifier,contentAlignment=Alignment.Center) {
        if(bitmap!=null) {
            Image(bitmap.asImageBitmap(),
                contentDescription=Wq2DMascots.label(id),
                contentScale=ContentScale.Fit,
                modifier=Modifier.fillMaxSize().scale(scale))
        }else {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(
                    listOf(color,Color(0xFFF8F1FF)))),
                contentAlignment=Alignment.Center) {
                Text(Wq2DMascots.label(id).take(1),fontSize=24.sp,
                    color=Color(0xFF513587),fontWeight=FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun Wq2DCompactSelector(
    selected:String,onClick:()->Unit,modifier:Modifier=Modifier
) {
    Row(modifier.clip(RoundedCornerShape(24.dp))
        .background(Color(0xFFF3EAFB))
        .border(1.dp,Color(0xFFE0CFFA),RoundedCornerShape(24.dp))
        .clickable(onClick=onClick)
        .padding(horizontal=9.dp,vertical=5.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        Wq2DPortrait(selected,Modifier.size(34.dp))
        Column {
            Text(Wq2DMascots.label(selected),fontSize=12.sp,
                fontWeight=FontWeight.Bold,color=Color(0xFF40236F))
            Text("Karakter Seç  ⌄",fontSize=10.sp,color=Color(0xFF754DA8))
        }
    }
}
