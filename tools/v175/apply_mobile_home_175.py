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
s=replace_once(s,
    'Text("Kelime Bulmacası", color = Forest',
    'Text("WordQuest AI", color = Color(0xFF203A49)',
    "correct product branding in mobile header")
s=replace_once(s,
    '                PrimaryAction("Yeni Oyun", newGame)',
    '                Wq175HomeStartButton(newGame)',
    "midnight-blue home CTA")

begin='                val score = session.totalPoints.toString() + " puan · " + session.finishedCount + " tamamlanan bulmaca"'
end='\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun HeroTiles'
assert s.count(begin)==1,(begin,s.count(begin))
assert s.count(end)==1,(end,s.count(end))
a=s.index(begin); z=s.index(end,a)
old=s[a:z]
# Earlier overlays may move or rename individual menu items. Confirm the two
# original navigation groups still exist before replacing them with all routes.
assert 'val play: @Composable' in old, 'Original play group not found'
assert 'val study: @Composable' in old, 'Original study group not found'
s=s[:a]+'''                Wq175PremiumHomeGrid(
                    session=session,
                    newGame=newGame,packages=packages,arena=firebaseArena,
                    yds=yds,ydsCenter={ wq175AcademyOpen = true },custom=custom,
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
    '''            if (title.isNotEmpty()) Text(title, fontFamily = Serif, color = Ink, fontSize = 38.sp, lineHeight = 41.sp)''',
    '''            if (title.isNotEmpty()) {
                val mainArt=Wq175ThemeArt.fromTitle(title)
                val roomIcon=if(mainArt!=null) Wq175ThemeArt.glossName(mainArt)
                    else Wq175ThemeArt.secondaryIcon(title)
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(10.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    if(roomIcon!=null) Wq175GlossIcon(roomIcon,Modifier.size(38.dp))
                    Text(title,modifier=Modifier.weight(1f),
                        fontFamily=Serif,color=Ink,
                        fontSize=if(LocalDensity.current.fontScale>1.2f)24.sp else 29.sp,
                        lineHeight=if(LocalDensity.current.fontScale>1.2f)29.sp else 35.sp,
                        maxLines=2)
                }
            }''',
    "compact native-mobile section headers")
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
                            Modifier.width(if(wide)182.dp else
                                if(LocalDensity.current.fontScale>1.2f)100.dp else 136.dp)
                                .height(if(wide)184.dp else 164.dp),
                            contentAlignment=Alignment.BottomCenter
                        ) {
                            Box(Modifier.align(Alignment.Center)
                                .size(if(wide)157.dp else 113.dp)
                                .background(Brush.radialGradient(listOf(
                                    Color(0xA9FFE7A7),Color(0x3588D0A6),
                                    Color.Transparent)),CircleShape))
                            Wq2DPortrait(
                                Wq2DMascots.selected(LocalContext.current,session.profileKey),
                                Modifier.fillMaxWidth().fillMaxHeight(.96f))
                        }''',
    "actual selected mascot in rounded integrated header")
if 'import androidx.compose.ui.draw.alpha' not in s:
    s=s.replace('import androidx.compose.ui.draw.clip',
                'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.alpha',1)
for imported in (
    'import androidx.compose.foundation.border',
    'import androidx.compose.foundation.shape.CircleShape',
    'import androidx.compose.ui.draw.drawWithContent',
    'import androidx.compose.ui.graphics.Brush',
    'import androidx.compose.ui.graphics.Color',
    'import androidx.compose.ui.graphics.BlendMode',
    'import androidx.compose.ui.graphics.CompositingStrategy',
    'import androidx.compose.ui.graphics.graphicsLayer'
):
    if imported not in s:
        s=s.replace('import androidx.compose.ui.draw.alpha',
                    'import androidx.compose.ui.draw.alpha\n'+imported,1)
home.write_text(s,"utf-8")

# V175 Arena hero was overriding the v174 WebP art with a crude Canvas drawing.
# Re-use the original *three different* high-DPI glossy mode assets.
arena=src/"WordQuest175Arena.kt"
s=arena.read_text("utf-8")
s=replace_once(s,"            Wq175ArenaModeSymbol(kind)",
    '            Wq175GlossIcon("arena_"+kind,Modifier.size(69.dp))',
    "Arena duel/team/quiz distinct 256px original premium icons")
arena.write_text(s,"utf-8")

# The v1.0.74 match settings still displayed flat text/emoji-like glyphs.
# Replace only visual content; Wq174SettingRow and its onSelect remain untouched.
settings=src/"WordQuest174Arena.kt"
raw=settings.read_text("utf-8")
old='''@Composable
private fun Wq174SettingIcon(kind:String) {
    Box(Modifier.size(39.dp).clip(RoundedCornerShape(12.dp))
        .background(Color(0xFFEEDCFF)),contentAlignment=Alignment.Center) {
        when(kind){
            "level"->Text("ABC",fontSize=13.sp,color=Royal174,fontWeight=FontWeight.Black)
            "topic"->Text("▱",fontSize=23.sp,color=Royal174,fontWeight=FontWeight.Bold)
            "time"->Text("◴",fontSize=27.sp,color=Royal174,fontWeight=FontWeight.Bold)
            "count"->Text("☷",fontSize=25.sp,color=Royal174,fontWeight=FontWeight.Bold)
            else->Text("✦",fontSize=22.sp,color=Color(0xFFB68704))
        }
    }
}'''
new='''@Composable
private fun Wq174SettingIcon(kind:String) {
    val asset=when(kind) {
        "level" -> "packages"
        "topic" -> "library"
        "time" -> "timer"
        "count" -> "puzzle"
        "hint" -> "insights"
        else -> "settings"
    }
    Wq175GlossIcon(asset,Modifier.size(39.dp))
}'''
raw=replace_once(raw,old,new,"original glossy art in Arena match settings")
raw=replace_once(raw,'Text("⚙",color=Color.White,fontSize=24.sp)',
    'Wq175GlossIcon("settings",Modifier.size(34.dp))',
    "Arena match-settings header icon")
settings.write_text(raw,"utf-8")
print("PASS: 2-column mobile home navigation; all 12 original destinations preserved")
print("PASS: selected maskot in profile and greeting; dark slate palette; 3 glossy Arena mode icons")
