package com.kelimebulmacasi.a1

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Only a small button is visible until this dialog is opened. */
@Composable
internal fun Wq2DCharacterPicker(
    selected:String,onSelected:(String)->Unit,onDismiss:()->Unit
) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var message by remember{mutableStateOf("")}
    var importing by remember{mutableStateOf(false)}
    var installed by remember{mutableStateOf(Wq2DMascots.available(context))}
    val launcher=rememberLauncherForActivityResult(
        contract=ActivityResultContracts.OpenDocument()
    ){uri->
        if(uri!=null && !importing) {
            importing=true
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { Wq2DMascots.importArt(context,uri) }
                }.onSuccess{
                    installed=true
                    message="20 özgün karakter görseli yüklendi."
                }.onFailure {
                    message="Görseller yüklenemedi: "+(it.message?:"Dosya okunamadı")
                }
                importing=false
            }
        }
    }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Karakter Seç",fontWeight=FontWeight.ExtraBold,
            color=Color(0xFF36205F))},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=480.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(9.dp)){
                Text("Seçtiğin karakter Arena ve Kelime Hafızam'da aynı olacak.",
                    fontSize=12.sp,color=Color(0xFF746B87))
                if(!installed) {
                    OutlinedButton(
                        onClick={launcher.launch(arrayOf("*/*"))},
                        enabled=!importing,
                        modifier=Modifier.fillMaxWidth()) {
                        Text(if(importing)"Görseller doğrulanıyor..."
                            else "Özgün 2D Görselleri Yükle")
                    }
                }
                if(message.isNotBlank())Text(message,
                    color=Color(0xFF6B478E),fontSize=11.sp)
                Wq2DMascots.ids.chunked(2).forEach{pair->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        pair.forEach{id->
                            val active=id==selected
                            Column(Modifier.weight(1f)
                                .clip(RoundedCornerShape(17.dp))
                                .background(if(active)Color(0xFFE9F9DF)
                                    else Color(0xFFF6F0FC))
                                .border(if(active)2.dp else 1.dp,
                                    if(active)Color(0xFF79BD49)
                                    else Color(0xFFE9DFF6),RoundedCornerShape(17.dp))
                                .clickable{onSelected(id);onDismiss()}
                                .padding(6.dp),
                                horizontalAlignment=Alignment.CenterHorizontally) {
                                Wq2DPortrait(id,Modifier.size(76.dp))
                                Text(Wq2DMascots.label(id),
                                    fontSize=12.sp,fontWeight=FontWeight.Bold,
                                    color=Color(0xFF36205F))
                            }
                        }
                    }
                }
                if(installed) {
                    Text("Kazanma pozu",fontSize=12.sp,
                        fontWeight=FontWeight.SemiBold,color=Color(0xFF493275))
                    Wq2DPortrait(selected,
                        Modifier.fillMaxWidth().height(125.dp),victory=true)
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Kapat")}},
        containerColor=Color.White
    )
}
