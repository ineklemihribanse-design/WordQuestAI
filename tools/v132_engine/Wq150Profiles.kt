package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Existing arena consent and selected art, separate bounded profile channel. */
@Composable
internal fun rememberWq150Profiles(room:String,session:AppSession,gateway:Wq150Gateway):JSONObject{
    val context=LocalContext.current
    val selected by rememberWq2DSelection(session.profileKey)
    val consent by rememberWq183ShareConsent(session.profileKey)
    var profiles by remember(room){mutableStateOf(JSONObject())}
    LaunchedEffect(room,selected,consent,session.profileKey){
        val custom=Wq183MascotStore.isCustomId(selected)
        val preview=if(custom&&consent)withContext(Dispatchers.Default){
            Wq183MascotStore.pictureForProfile(context,session.profileKey,selected,false)?.let{image->
                try{Wq128RoomHd.encode(selected,image)}finally{image.recycle()}
            }
        }else null
        val avatar=if(custom&&preview==null)"piko" else selected
        repeat(3){
            try{gateway.publishPortrait(room,avatar,preview);return@LaunchedEffect}
            catch(e:CancellationException){throw e}
            catch(e:Exception){delay(1500L)}
        }
    }
    LaunchedEffect(room,gateway){
        var version:String?=null
        while(true){
            try{
                val response=gateway.profiles(room,version)
                response.optJSONObject("profiles")?.let{profiles=it}
                version=response.optString("version").takeIf{it.matches(Regex("^[a-f0-9]{64}$"))}
            }catch(e:CancellationException){throw e}
            catch(e:Exception){}
            delay(8000L)
        }
    }
    return profiles
}
@Composable
internal fun Wq150Portrait(profile:JSONObject?,ownAvatar:String?,modifier:Modifier=Modifier,victory:Boolean=false){
    val id=profile?.optString("avatar","piko")?:"piko"
    val preview=profile?.optString("preview")?.takeIf{it.isNotBlank()&&it!="null"}
    val bitmap by produceState<Bitmap?>(null,id,preview){
        value=withContext(Dispatchers.Default){Wq128RoomHd.decode(id,preview)}
    }
    val image=bitmap
    if(ownAvatar!=null)Wq2DPortrait(ownAvatar,modifier,victory)
    else if(image!=null)Image(image.asImageBitmap(),contentDescription="Oyuncu maskotu",modifier=modifier,
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.High)
    else Wq2DPortrait(if(id in Wq2DMascots.ids)id else "piko",modifier,victory)
}
