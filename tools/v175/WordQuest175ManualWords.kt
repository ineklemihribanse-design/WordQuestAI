package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Word Memory's add flow writes to the existing LearningStore of the active
 * player. This first stage adds verified manual entry, not fake PDF/OCR buttons.
 * OCR/PDF/camera/AI ingestion follows the same preview/approval contract.
 */
@Composable
internal fun Wq175AddWordDialog(
    onDismiss:()->Unit,
    add:(String,String,String)->Boolean,
    error:()->String?
) {
    var english by remember { mutableStateOf("") }
    var turkish by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var saved by remember { mutableIntStateOf(0) }
    val valid=english.trim().length in 2..75 &&
        turkish.trim().length in 2..140 &&
        english.all { it.isLetter() || it==' ' || it=='-' || it=='\'' }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Kelime Defterime Ekle",fontWeight=FontWeight.ExtraBold,
            color=Color(0xFF173D64))},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
                Text("Kelimeler, öğrenme geçmişinin bulunduğu mevcut özel deftere kaydedilir.",
                    fontSize=12.sp,color=Color(0xFF61708A))
                OutlinedTextField(english,
                    { english=it.take(75);status="" },
                    label={Text("İngilizce kelime veya ifade")},
                    modifier=Modifier.fillMaxWidth(),singleLine=true)
                OutlinedTextField(turkish,
                    { turkish=it.take(140);status="" },
                    label={Text("Türkçe karşılığı")},
                    modifier=Modifier.fillMaxWidth(),singleLine=true)
                OutlinedTextField(example,
                    { example=it.take(300);status="" },
                    label={Text("Örnek / not (isteğe bağlı)")},
                    modifier=Modifier.fillMaxWidth(),maxLines=3)
                if(status.isNotBlank()) {
                    Text(status,fontSize=12.sp,
                        color=if(status.startsWith("Eklendi"))Color(0xFF27764F)
                        else Color(0xFFB0474C))
                }
                if(saved>0) Text("$saved kelime başarıyla kaydedildi",
                    color=Color(0xFF23734C),fontWeight=FontWeight.SemiBold,
                    fontSize=12.sp)
                Button(
                    enabled=valid,
                    modifier=Modifier.fillMaxWidth(),
                    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF348ED0)),
                    onClick={
                        if(add(english.trim(),turkish.trim(),example.trim())) {
                            saved++
                            english="";turkish="";example=""
                            status="Eklendi. Yeni kelime girebilirsin."
                        } else {
                            status=error()?:"Bu kelime zaten defterinde bulunuyor."
                        }
                    }
                ) { Text("Kelimeyi Kaydet",color=Color.White) }
            }
        },
        confirmButton={
            TextButton(onClick=onDismiss) {
                Text("Kapat",color=Color(0xFF69359A))
            }
        },
        containerColor=Color.White
    )
}

@Composable
internal fun Wq175AddWordEntry(onClick:()->Unit) {
    Surface(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=57.dp),
        shape=RoundedCornerShape(19.dp),
        color=Color(0xFFF2EDFF),
        border=androidx.compose.foundation.BorderStroke(1.dp,
            Color(0xFFDECEFA))) {
        Row(Modifier.padding(horizontal=14.dp,vertical=11.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Box(Modifier.size(35.dp)
                .background(Color(0xFFDED0FF),RoundedCornerShape(12.dp)),
                contentAlignment=Alignment.Center) {
                Text("+",color=Color(0xFF6836B0),fontSize=25.sp,
                    fontWeight=FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text("Kendi Kelimelerimi Ekle",
                    color=Color(0xFF382262),fontWeight=FontWeight.Bold,
                    fontSize=14.sp)
                Text("İngilizce, Türkçe ve örnek cümle",
                    color=Color(0xFF79688D),fontSize=11.sp)
            }
            Text("›",color=Color(0xFF714CB1),fontSize=25.sp)
        }
    }
}
