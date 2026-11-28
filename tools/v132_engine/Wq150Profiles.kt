package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Read only the existing signed-in public nickname directory for this match's roster. */
@Composable
internal fun rememberWq150ArenaNames(session:AppSession,ids:List<String>):Map<String,String>{
    val auth=session.accounts.socialAuth()
    val uid=session.accounts.uid.orEmpty()
    var names by remember(uid,ids){mutableStateOf<Map<String,String>>(emptyMap())}
    DisposableEffect(auth,uid,ids){
        val root=if(auth==null||uid.isBlank())null else runCatching{
            FirebaseDatabase.getInstance(auth.app,"https://wordquest-ai-default-rtdb.firebaseio.com").reference.child("directory")
        }.getOrNull()
        val refs=if(root==null)emptyList() else ids.distinct().filter{it.matches(Regex("^[A-Za-z0-9_-]{1,100}$"))&&it!=uid}.map{id->
            val ref=root.child(id).child("nickname")
            val listener=object:ValueEventListener{
                override fun onDataChange(snapshot:DataSnapshot){
                    val name=snapshot.getValue(String::class.java)?.trim().orEmpty()
                    if(ArenaNickname.isValid(name))names=names+(id to name)
                }
                override fun onCancelled(error:DatabaseError){}
            }
            ref.addValueEventListener(listener)
            ref to listener
        }
        onDispose{refs.forEach{(ref,listener)->ref.removeEventListener(listener)}}
    }
    return names
}

/** Existing arena consent and selected art, separate bounded profile channel. */
@Composable
internal fun rememberWq150Profiles(room:String,session:AppSession,gateway:Wq150Gateway):JSONObject{
    val context=LocalContext.current
    val selected by rememberWq2DSelection(session.profileKey)
    val consent by rememberWq183ShareConsent(session.profileKey)
    var profiles by remember(room,session.profileKey,gateway){mutableStateOf(JSONObject())}
    // Serialize sends across selection/consent changes so an older in-flight
    // upload cannot overwrite the newest selection after a cancellation.
    val publishLock=remember(room,session.profileKey,gateway){Mutex()}
    LaunchedEffect(room,selected,consent,session.profileKey,session.profileName,gateway){
        val custom=Wq183MascotStore.isCustomId(selected)
        var retryDelay=1500L
        while(isActive){
            try{
                val preview=if(custom&&consent)withContext(Dispatchers.Default){
                    Wq183MascotStore.pictureForProfile(context,session.profileKey,selected,false)?.let{image->
                        try{Wq128RoomHd.encode(selected,image)}finally{image.recycle()}
                    }
                }else null
                val victory=if(custom&&consent&&preview!=null)withContext(Dispatchers.Default){
                    Wq183MascotStore.pictureForProfile(context,session.profileKey,selected,true)?.let{image->
                        try{Wq128RoomHd.encode(selected,image)}finally{image.recycle()}
                    }
                }else null
                val avatar=if(custom&&preview==null)"piko" else selected
                publishLock.withLock{gateway.publishPortrait(room,avatar,preview,victory)}
                // A temporary read/encode failure must not permanently replace
                // a consented custom portrait with the default avatar.
                if(!custom||!consent||preview!=null)break
            }catch(e:CancellationException){throw e}
            catch(e:Exception){}
            delay(retryDelay)
            retryDelay=(retryDelay*2).coerceAtMost(15000L)
        }
    }
    LaunchedEffect(room,gateway,session.profileKey){
        var version:String?=null
        var pollDelay=8000L
        while(isActive){
            try{
                val response=gateway.profiles(room,version)
                val nextVersion=response.optString("version").takeIf{it.matches(Regex("^[a-f0-9]{64}$"))}
                val next=response.optJSONObject("profiles")
                if(next!=null){profiles=next;version=nextVersion}
                else if(nextVersion==null||nextVersion!=version)version=null
                pollDelay=8000L
            }catch(e:CancellationException){throw e}
            catch(e:Exception){version=null;pollDelay=4000L}
            delay(pollDelay)
        }
    }
    return profiles
}
@Composable
internal fun Wq150Portrait(profile:JSONObject?,ownAvatar:String?,modifier:Modifier=Modifier,victory:Boolean=false){
    val id=profile?.optString("avatar","piko")?:"piko"
    val idle=profile?.optString("preview")?.takeIf{it.isNotBlank()&&it!="null"}
    val win=profile?.optString("victory")?.takeIf{it.isNotBlank()&&it!="null"}
    val preview=if(victory)win?:idle else idle
    var bitmap by remember(id,preview){mutableStateOf<Bitmap?>(null)}
    LaunchedEffect(id,preview){
        bitmap=withContext(Dispatchers.Default){Wq128RoomHd.decode(id,preview)?:if(victory)Wq128RoomHd.decode(id,idle) else null}
    }
    val image=bitmap
    if(ownAvatar!=null)Wq2DPortrait(ownAvatar,modifier,victory)
    else if(image!=null)Image(image.asImageBitmap(),contentDescription="Oyuncu maskotu",modifier=modifier,
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.High)
    else Wq2DPortrait(if(id in Wq2DMascots.ids)id else "piko",modifier,victory)
}
