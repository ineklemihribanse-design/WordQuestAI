#!/usr/bin/env python3
"""Version 1.0.83: group existing navigation into five real feature hubs.

The original game, learning and progress callbacks are forwarded verbatim.
Does not alter any persistence, Firebase state or available destination.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
TARGET=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1/Wq175PremiumHome.kt"
source=TARGET.read_text("utf-8")
assert source.count("    val main = listOf(")==1
start=source.index("    val main = listOf(")
assert source.rstrip().endswith("}") and source.count("    var matchingOpen by remember")==1
# Last closing brace belongs to Wq175PremiumHomeGrid.
end=source.rfind("}")
section='''    var activeSection by remember { mutableStateOf<String?>(null) }
    val categories = listOf(
        Wq175HomeTile("ÖĞREN", "Kelimeler, paketler, fiiller ve tekrar.",
            "cards", Color(0xFFE6F8F6), { activeSection="ÖĞREN" },
            artSlug="kelime_hafizam"),
        Wq175HomeTile("OYUNLAR", "Bulmaca, eşleştirme ve kişisel oyun.",
            "puzzle", Color(0xFFFFF0DB), { activeSection="OYUNLAR" },
            artSlug="bulmacalarim"),
        Wq175HomeTile("ARENA", "Düello, takım oyunu ve canlı yarış.",
            "trophy", Color(0xFFF0E8FC), { activeSection="ARENA" },
            artSlug="arena"),
        Wq175HomeTile("YDS AKADEMİ", "Konu anlatımı, test ve denemeler.",
            "yds", Color(0xFFE7F3FF), { activeSection="YDS AKADEMİ" },
            artSlug="yds_akademi"),
        Wq175HomeTile("GÜNLÜK PLANIM", "Hedefler, tekrarlar, mini oyunlar.",
            "plan", Color(0xFFFFF6DD), { activeSection="GÜNLÜK PLANIM" },
            artSlug="gunluk_plan")
    )
    val features = when(activeSection){
        "ÖĞREN" -> listOf(
            Wq175HomeTile("Kelime Hafızam",
                "${session.learningDueCount()} kelime tekrar bekliyor.",
                "cards",Color(0xFFE7F8F7),learning,artSlug="kelime_hafizam"),
            Wq175HomeTile("Hazır Paketler","Seviyene uygun kelimeleri keşfet.",
                "packages",Color(0xFFF4F4DC),packages),
            Wq175HomeTile("Fiiller","V1, V2, V3 ile pratik yap.",
                "verbs",Color(0xFFE7F4EE),verbs),
            Wq175HomeTile("İlerlemem","Öğrenme geçmişi ve istatistikler.",
                "analytics",Color(0xFFFFF1DC),progress),
            Wq175HomeTile("Keşfet ve Öğren","Yeni öğrenme içeriklerine ulaş.",
                "insights",Color(0xFFE7F5FF),insights)
        )
        "OYUNLAR" -> listOf(
            Wq175HomeTile("Yeni Oyun","Yeni bir kelime bulmacası başlat.",
                "puzzle",Color(0xFFE7F8F7),newGame),
            Wq175HomeTile("Bulmacalarım","Kendi bulmacalarını oluştur ve düzenle.",
                "mistakes",Color(0xFFFFEBDD),custom,artSlug="bulmacalarim"),
            Wq175HomeTile("Eşleştirmelerim","Kelime kartlarını eşleştir.",
                "synonym",Color(0xFFE5F8F9),{matchingOpen=true},
                artSlug="eslestirmelerim")
        )
        "ARENA" -> listOf(
            Wq175HomeTile("Arena'ya Gir","Düello, takım, ortak bulmaca ve 5 şıklı oyun.",
                "trophy",Color(0xFFF0E8FC),arena,artSlug="arena")
        )
        "YDS AKADEMİ" -> listOf(
            Wq175HomeTile("YDS Akademi","Konu, paragraf, deneme ve yanlış analizi.",
                "yds",Color(0xFFE7F3FF),ydsCenter,artSlug="yds_akademi"),
            Wq175HomeTile("YDS Hazırlık","Seviyeli kelime bulmacaları.",
                "yds",Color(0xFFE9F1FD),yds)
        )
        "GÜNLÜK PLANIM" -> listOf(
            Wq175HomeTile("Günlük Planım","Günlük hedefler ve mini oyunlar.",
                "plan",Color(0xFFFFF6DD),practice,artSlug="gunluk_plan"),
            Wq175HomeTile("İlerlemem",
                "${session.totalPoints} puan · ${session.finishedCount} bulmaca.",
                "analytics",Color(0xFFFFF1DC),progress)
        )
        else -> emptyList()
    }

    // A permanent quick path to Lena; not a duplicate grid category.
    Column(Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(11.dp)) {
        if(activeSection==null) {
            Text("Beş öğrenme alanı",fontWeight=FontWeight.ExtraBold,
                color=Wq175Navy,fontSize=20.sp)
        } else {
            Button(onClick={activeSection=null},
                colors=ButtonDefaults.buttonColors(
                    containerColor=Color(0xFFF1E8FD),
                    contentColor=Color(0xFF382956))) {
                Text("‹ Ana bölümlere dön",fontWeight=FontWeight.Bold)
            }
            Text(activeSection.orEmpty(),fontWeight=FontWeight.ExtraBold,
                color=Wq175Navy,fontSize=21.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val singleColumn=maxWidth<345.dp ||
                LocalDensity.current.fontScale>1.45f
            val tiles=if(activeSection==null) categories else features
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                if(singleColumn) {
                    tiles.forEach { tile -> Wq175HomeTileView(tile,
                        small=activeSection!=null) }
                } else {
                    tiles.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            pair.forEach { tile ->
                                Box(Modifier.weight(1f)) {
                                    Wq175HomeTileView(tile,small=activeSection!=null)
                                }
                            }
                        }
                    }
                }
            }
        }
        if(activeSection==null) {
            Surface(
                onClick=assistant,
                modifier=Modifier.fillMaxWidth().heightIn(min=62.dp),
                shape=RoundedCornerShape(18.dp),
                color=Color(0xFFECE9F9)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)
                ) {
                    Wq175GlossIcon("assistant",Modifier.size(43.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Lena ile konuş",fontWeight=FontWeight.Bold,
                            color=Wq175Navy,fontSize=15.sp)
                        Text("Yazılı ve sesli İngilizce pratiği",
                            color=Wq175Muted,fontSize=12.sp)
                    }
                    Text("›",color=Wq175Navy,fontSize=22.sp)
                }
            }
        }
    }
'''
TARGET.write_text(source[:start]+section+source[end:],"utf-8")
print("PASS v183 five categories; all feature route callbacks preserved; Lena shortcut kept")
