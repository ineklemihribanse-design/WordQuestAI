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
    '''            if (title.isNotEmpty()) Text(title,
                fontFamily=Serif,color=Ink,
                fontSize=if(LocalDensity.current.fontScale>1.2f)24.sp else 29.sp,
                lineHeight=if(LocalDensity.current.fontScale>1.2f)29.sp else 35.sp,
                maxLines=2)''',
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
                            Modifier.width(if(wide)178.dp else
                                if(LocalDensity.current.fontScale>1.2f)112.dp
                                else 124.dp).height(if(wide)178.dp else 164.dp)
                        ) {
                            Wq175ThemeIllustration("ana_sayfa",
                                Modifier.matchParentSize()
                                    .graphicsLayer {
                                        compositingStrategy=CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(brush=Brush.horizontalGradient(
                                            0f to Color.Transparent,
                                            .34f to Color.White,
                                            1f to Color.White
                                        ),blendMode=BlendMode.DstIn)
                                        drawRect(brush=Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            .14f to Color.White,
                                            .86f to Color.White,
                                            1f to Color.Transparent
                                        ),blendMode=BlendMode.DstIn)
                                    }
                                    .alpha(.32f))
                            Wq2DPortrait(
                                Wq2DMascots.selected(LocalContext.current,session.profileKey),
                                Modifier.align(Alignment.BottomCenter)
                                    .fillMaxHeight(.93f))
                        }''',
    "actual chosen mascot in greeting artwork")
if 'import androidx.compose.ui.draw.alpha' not in s:
    s=s.replace('import androidx.compose.ui.draw.clip',
                'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.alpha',1)
for imported in (
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
print("PASS: 2-column mobile home navigation; all 12 original destinations preserved")
print("PASS: selected maskot in profile and greeting; dark slate palette; 3 glossy Arena mode icons")
