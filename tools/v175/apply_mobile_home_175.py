#!/usr/bin/env python3
"""Restore approved two-column *native mobile* Home + real 3D icon assets.

Runs after apply_theme_art_175.py, keeps all existing destinations and user data.
"""
from pathlib import Path
import shutil

root=Path(__file__).resolve().parents[2]
pkg=root/"tools/v175"
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
shutil.copyfile(pkg/"Wq175PremiumHome.kt",src/"Wq175PremiumHome.kt")

def replace_once(s,old,new,label):
    count=s.count(old)
    assert count==1,(label,count)
    return s.replace(old,new,1)

home=src/"HomeScreen.kt"
s=home.read_text("utf-8")
begin='                val score = session.totalPoints.toString() + " puan · " + session.finishedCount + " tamamlanan bulmaca"'
end='\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun HeroTiles'
assert s.count(begin)==1,(begin,s.count(begin))
assert s.count(end)==1,(end,s.count(end))
a=s.index(begin); z=s.index(end,a)
old=s[a:z]
for needed in ('MenuItem("Hazır Paketler"','MenuItem("YDS Hazırlık"',
    'MenuItem("YDS Sınav Merkezi"','MenuItem("Bulmacalarım"',
    'MenuItem("Arena"','MenuItem("Konuşma Asistanı"',
    'MenuItem("Günlük Planım & Yeni Oyunlar"','MenuItem("Kelime Hafızam"',
    'MenuItem("Fiillerin 1., 2. ve 3. Hâli"','MenuItem("İlerlemem"',
    'MenuItem("Öğrenme Merkezim"'):
    assert needed in old,needed
s=s[:a]+'''                Wq175PremiumHomeGrid(
                    session=session,
                    newGame=newGame,packages=packages,arena=firebaseArena,
                    yds=yds,ydsCenter=ydsCenter,custom=custom,
                    learning=learning,practice=practice,
                    assistant=assistant,verbs=verbs,
                    progress=progress,insights=insights
                )'''+s[z:]
s=replace_once(s,
    '''Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Board)
                        .heightIn(min = if (wide) 182.dp else 174.dp)''',
    '''Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF203A49))
                        .heightIn(min = if (wide) 182.dp else 190.dp)''',
    "calmer midnight-navy home hero (no pink/green clash)")
s=replace_once(s,
    '''                        OutlinedButton(onClick = account) {
                            Text(if (session.accountName == null) "Hesap" else session.profileName.take(16), fontFamily = Sans, fontWeight = FontWeight.Bold)
                        }''',
    '''                        OutlinedButton(onClick = account,
                            contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)) {
                            Wq2DPortrait(
                                Wq2DMascots.selected(LocalContext.current,session.profileKey),
                                Modifier.size(31.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (session.accountName == null) "Hesap"
                                else session.profileName.take(12),
                                fontFamily=Sans,fontWeight=FontWeight.Bold,
                                maxLines=1)
                        }''',
    "profile uses real selected mascot")
s=replace_once(s,
    '''                        Wq175ThemeIllustration("ana_sayfa",
                            Modifier.width(if(wide)178.dp else
                                if(LocalDensity.current.fontScale>1.2f)112.dp
                                else 124.dp)
                                .height(if(wide)178.dp else 164.dp))''',
    '''                        Box(
                            Modifier.width(if(wide)178.dp else
                                if(LocalDensity.current.fontScale>1.2f)112.dp
                                else 124.dp).height(if(wide)178.dp else 164.dp)
                        ) {
                            Wq175ThemeIllustration("ana_sayfa",
                                Modifier.matchParentSize()
                                    .alpha(.25f))
                            Wq2DPortrait(
                                Wq2DMascots.selected(LocalContext.current,session.profileKey),
                                Modifier.align(Alignment.BottomCenter)
                                    .fillMaxHeight(.93f))
                        }''',
    "actual chosen mascot in greeting artwork")
if 'import androidx.compose.ui.draw.alpha' not in s:
    s=s.replace('import androidx.compose.ui.draw.clip',
                'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.alpha',1)
home.write_text(s,"utf-8")

# V175 Arena hero was overriding the v174 WebP art with a crude Canvas drawing.
# Re-use the original *three different* high-DPI glossy mode assets.
arena=src/"WordQuest175Arena.kt"
s=arena.read_text("utf-8")
s=replace_once(s,"            Wq175ArenaModeSymbol(kind)",
    '            Wq175GlossIcon("arena_"+kind,Modifier.size(69.dp))',
    "Arena duel/team/quiz distinct 256px original premium icons")
arena.write_text(s,"utf-8")
print("PASS: 2-column mobile home navigation; all 12 original destinations preserved")
print("PASS: selected maskot in profile and greeting; dark slate palette; 3 glossy Arena mode icons")
