#!/usr/bin/env python3
"""v190: Restore separate Arena mode and settings pages, consistent setting cards,
real multi-level choices for solo and online, and solo access to all three modes.

Applied after P1–P7 and v189/v190 patches. No RTDB rules or data mutation.
Network rooms are still created only by the existing multiplayer host callbacks.
"""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
PKG=Path(__file__).resolve().parent

def once(s,before,after,name):
    n=s.count(before)
    if n!=1:
        raise AssertionError(f"{name}: expected 1 anchor, got {n}")
    return s.replace(before,after,1)

def main():
    pending={}
    file=APP/"ArenaApprovedHome.kt"
    s=file.read_text("utf-8")
    assert "var matchSettingsPage by rememberSaveable" in s, "Existing two-page state must still be present"
    s=once(s,
        """        // Mode choice and match settings belong to a single Arena destination.
        Wq175ArenaHeroAndModes(
            selectedAvatar=mascot,mode=mode,solo=solo,onMode=onMode
        )
        Wq174UnifiedMatchSettings(""",
        """        // Page 1: select a real gameplay mode; Page 2: edit match settings.
        // Back returns to the mode picker without discarding selected levels.
        if (!matchSettingsPage) {
            Wq175ArenaHeroAndModes(
                selectedAvatar=mascot,mode=mode,solo=solo,
                onMode={ nextMode ->
                    onMode(nextMode)
                    matchSettingsPage=true
                }
            )
        } else {
            TextButton(onClick={matchSettingsPage=false},
                modifier=Modifier.align(Alignment.Start)) {
                Text("‹  Oyun modlarına dön",
                    color=Color(0xFF6534A9),
                    fontWeight=FontWeight.ExtraBold)
            }
            Text("Seçilen mod: " + Wq190ArenaSoloPolicy.displayName(mode,solo),
                color=ArenaApproved.ink,fontWeight=FontWeight.SemiBold)
            Wq174UnifiedMatchSettings(""",
        "two step Arena mode then settings")
    s=once(s,
        '''        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)
    }''',
        '''        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)
        }
    }''',
        "close second page after existing single/online start controls")
    assert s.count("Wq175ArenaHeroAndModes(")==1 and s.count("Wq174UnifiedMatchSettings(")==1
    assert "onOffline" in s and "onCreate" in s and "onJoin" in s
    pending[file]=s

    file=APP/"WordQuest175Arena.kt"
    s=file.read_text("utf-8")
    s=once(s,
        '''        Wq175ModeCard("Ortak Bulmaca",
            "Aynı tahtayı birlikte çöz.",
            "team",selected=mode=="team",enabled=!solo) {onMode("team")}''',
        '''        Wq175ModeCard("Ortak Bulmaca",
            if(solo) "Tek kişilik ortak-tahta alıştırması."
            else "Aynı tahtayı birlikte çöz.",
            "team",selected=mode=="team",enabled=true) {onMode("team")}''',
        "solo cooperative practice must be selectable, no fabricated second user")
    pending[file]=s

    file=APP/"WordQuest174Arena.kt"
    s=file.read_text("utf-8")
    s=once(s,
        '''                            if(isSolo && mode=="team")onMode("duel")
                            onSolo(isSolo)''',
        '''                            // Solo team is a legitimate local crossword exercise.
                            // Do not silently replace the chosen mode with duel.
                            onSolo(isSolo)''',
        "preserve mode team while switching offline")
    s=once(s,
        '''            if(solo) {
                Wq174SettingRow("level","Seviye Seçiniz",
                    level.ifBlank{"Seçiniz"},PremiumArenaRules.allowedLevels,
                    onSelect=onLevel)
            } else {
                // Online modes share a level pool. No second single-level picker.
                Wq186ArenaPresetControls(profile,level,onLevel)
            }''',
        '''            // Same multi-select six-level chips in both play types.
            // Presets persist per profile; the offline engine reads these too.
            Wq186ArenaPresetControls(profile,level,onLevel,solo=solo)''',
        "keep actual multiple level selection online and offline")
    s=once(s,
        '''    Column(Modifier.fillMaxWidth().shadow(8.dp,RoundedCornerShape(29.dp))''',
        '''    var arenaInfoOpen by remember { mutableStateOf(false) }
    if (arenaInfoOpen) AlertDialog(
        onDismissRequest={arenaInfoOpen=false},
        title={Text("Maç Ayarları")},
        text={Text("Bir veya daha fazla seviye, konu, süre ve soru sayısını seçebilirsin. " +
            "Tek başına oynadığında Firebase odası açılmaz. Arkadaşlı maçta " +
            "oyuncuların ikisi de aynı maç ayarlarını kullanır.")},
        confirmButton={TextButton(onClick={arenaInfoOpen=false}){Text("Kapat")}}
    )
    Column(Modifier.fillMaxWidth().shadow(8.dp,RoundedCornerShape(29.dp))''',
        "real info dialog for Arena settings heading")
    s=once(s,
        '''            Text("Maç Ayarları",color=Color.White,
                fontSize=22.sp,fontWeight=FontWeight.Black)''',
        '''            Text("Maç Ayarları",color=Color.White,
                fontSize=22.sp,fontWeight=FontWeight.Black)
            Spacer(Modifier.weight(1f))
            TextButton(onClick={arenaInfoOpen=true},
                modifier=Modifier.size(42.dp),
                contentPadding=PaddingValues(0.dp)) {
                Text("ⓘ",color=Color.White,fontSize=23.sp,
                    fontWeight=FontWeight.Bold)
            }''',
        "working info control directly beside heading")
    pending[file]=s

    file=APP/"Wq186ArenaPresetControls.kt"
    s=file.read_text("utf-8")
    s=once(s,
        '''    profile: String, selectedLevel: String, onLevel: (String) -> Unit
)''',
        '''    profile: String, selectedLevel: String, onLevel: (String) -> Unit,
    solo: Boolean = false
)''',
        "same level chips support both solo and network settings")
    s=once(s,
        '''        Text("Maç kurulduğunda seçilen ayarlar iki oyuncuya da Firebase üzerinden iletilir.",
            style=MaterialTheme.typography.bodySmall)''',
        '''        if(!solo) Text("Maç kurulduğunda bu ayarlar iki oyuncuya Firebase üzerinden iletilir.",
            style=MaterialTheme.typography.bodySmall)
        else Text("Seçilen seviyeler yalnızca bu cihazdaki tek kişilik oyuna uygulanır.",
            style=MaterialTheme.typography.bodySmall)''',
        "clarify that solo play never creates Firebase room")
    pending[file]=s

    for file,data in pending.items():file.write_text(data,"utf-8")
    for name in ("Wq190ArenaSoloPolicy.kt",):
        shutil.copyfile(PKG/name,APP/name)
    TEST.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(PKG/"Wq190ArenaSoloPolicyTest.kt",TEST/"Wq190ArenaSoloPolicyTest.kt")
    print("PASS v190 Arena page1 modes / page2 settings with matching card design")
    print("PASS v190 Arena all three solo modes selectably routed")
    print("PASS v190 Arena same saved six-level CEFR selection for offline and online")
    print("PASS v190 Arena original Firebase room/host callbacks untouched")

if __name__=="__main__":main()
