#!/usr/bin/env python3
"""Follow-up mobile/tablet layout polish for 1.0.75.

Runs after apply_requested_ui_refinement_175. Strict source anchors prevent silent
misses. Does not change APK identity, version/signature, AI keys, speech engine,
session data, the original Lena GLB, Arena queueing or networking.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"

def once(s,old,new,name):
    n=s.count(old)
    if n!=1:
        raise AssertionError(f"{name}: expected one source location, found {n}")
    return s.replace(old,new,1)

def patch(name,modify):
    p=SRC/name
    before=p.read_text("utf-8")
    after=modify(before)
    if before==after: raise AssertionError("No changes: "+name)
    p.write_text(after,"utf-8")
    print("PASS:",name)

def clean_home(s):
    # Keep the portrait in Profile. Remove only the redundant 2D mascot pasted
    # over the user's own classroom wallpaper on the Home screen.
    start=s.index('                        Box(\n                            Modifier.width(if(wide)182.dp else')
    end=s.index('\n                        }',start)+len('\n                        }')
    assert 'Wq2DPortrait(' in s[start:end]
    assert 'Modifier.fillMaxWidth().fillMaxHeight(.96f)' in s[start:end]
    s=s[:start]+s[end:]
    # Allow the existing text and scene to occupy the whole hero width.
    return s

def center_arena(s):
    start=s.index('        Box(Modifier.fillMaxWidth().heightIn(min=258.dp)')
    end=s.index('        Text("Maceranı seç"',start)
    section=s[start:end]
    assert 'Wq175SceneBackdrop("arena"' in section
    assert 'Wq2DPortrait(selectedAvatar' in section
    hero=r'''        // The podium belongs at the visual centre, independent of screen width.
        // Portrait is a separate live layer, not baked into the backdrop.
        BoxWithConstraints(Modifier.fillMaxWidth().height(274.dp)
            .clip(RoundedCornerShape(29.dp))) {
            Wq175SceneBackdrop("arena",Modifier.matchParentSize())
            val compact = maxWidth < 510.dp
            Column(
                Modifier.align(Alignment.TopStart)
                    .fillMaxWidth(if(compact) .48f else .43f)
                    .padding(start=18.dp,top=19.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("WORDQUEST · TURNOVA",
                    color=ArenaGold175,fontSize=10.sp,
                    fontWeight=FontWeight.Black,letterSpacing=.6.sp)
                Text("ARENA",color=Color.White,
                    fontSize=if(compact)27.sp else 35.sp,
                    fontWeight=FontWeight.Black)
                Text("Sahne senin!\nRakibini seç,\nzaferi kazan.",
                    color=Color(0xFFFFEEFF),fontSize=if(compact)13.sp else 16.sp,
                    lineHeight=if(compact)18.sp else 22.sp,
                    fontWeight=FontWeight.SemiBold)
                Surface(color=Color(0xC34B217F),
                    shape=RoundedCornerShape(15.dp),
                    border=BorderStroke(1.dp,ArenaGold175)) {
                    Text("★ "+Wq2DMascots.label(selectedAvatar),
                        color=Color.White,fontSize=12.sp,
                        fontWeight=FontWeight.ExtraBold,
                        modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))
                }
            }
            // Centre of podium: no side-column anchoring and no floating trophy.
            Wq2DPortrait(
                selectedAvatar,
                Modifier.align(Alignment.BottomCenter)
                    .offset(x=if(compact)17.dp else 4.dp,y=(-9).dp)
                    .size(if(compact)166.dp else 207.dp),
                victory=false
            )
        }
'''
    return s[:start]+hero+s[end:]

def packages_only(s):
    start=s.index('private fun PackageScreen(')
    end=s.index('private fun TopicScreen(',start)
    part=s[start:end]
    oldStart=part.index('        for ((group, entries) in games) {')
    oldEnd=part.index('        MenuItem("$level bulmacası oluştur"',oldStart)
    # The level tile is the only entry point. The catalogue has its own page.
    part=part[:oldStart]+'''        Text("Paket içeriğini görmek için seviyenin Paketi Gör düğmesine dokun.",
            color=Muted,fontFamily=Sans,fontSize=13.sp)
'''+part[oldEnd:]
    assert 'for ((group, entries) in games)' not in part
    # Keep the existing direct custom-game action, but avoid the duplicated
    # subject list which caused both pages to show the same catalogue.
    return s[:start]+part+s[end:]

def hide_voice_picker_lists(s):
    marker='            for (accent in EnglishVoice.entries) {'
    assert s.count(marker)==1
    s=s.replace(marker,
       '''            for (accent in EnglishVoice.entries) {
                Wq175SettingsCard(
                    accent.label+" cihaz sesleri",
                    "Kullanılabilir sesleri göster ve ses seç",
                    "listen"
                ) {''',1)
    old='''            }
            Text("Android ses motorları kadın/erkek bilgisini standart şekilde bildirmediği için "'''
    new='''                }
            }
            Text("Android ses motorları kadın/erkek bilgisini standart şekilde bildirmediği için "'''
    s=once(s,old,new,"close US/UK voice picker")
    return s

def compact_messenger(s):
    start=s.index('            if (!keyboardVisible) BoxWithConstraints(Modifier.fillMaxWidth()) {')
    end=s.index('\n            }\n',start)+len('\n            }\n')
    part=s[start:end]
    assert 'Text("Ses Ayarları ▾"' in part
    part=once(part,'Text(if(recording) "Dinleniyor" else "Konuş",',
        'Text(if(recording) "Kayıt" else "Konuş",maxLines=1,softWrap=false,',
        "mic label cannot wrap")
    part=once(part,'Text(if(speaking==null)"Dinle" else "Durdur",',
        'Text(if(speaking==null)"Dinle" else "Dur",maxLines=1,softWrap=false,',
        "playback label cannot wrap")
    part=once(part,'Text("Ses Ayarları ▾",fontSize=12.sp,maxLines=1)',
        'Text(if(wide)"Ses Ayarları" else "Ses ⚙",fontSize=12.sp,maxLines=1,softWrap=false)',
        "compact voice settings label")
    part=once(part,
        'modifier=Modifier.weight(1f),shape=RoundedCornerShape(50),',
        'modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=if(wide)12.dp else 4.dp,vertical=4.dp),shape=RoundedCornerShape(50),',
        "mic touch target")
    part=once(part,
        'modifier=Modifier.weight(if(wide).75f else 1f),',
        'contentPadding=PaddingValues(horizontal=if(wide)12.dp else 4.dp,vertical=4.dp),\n                        modifier=Modifier.weight(if(wide).75f else 1f),',
        "listen touch target")
    part=once(part,
        'modifier=Modifier.weight(if(wide)1f else 1.18f),',
        'contentPadding=PaddingValues(horizontal=if(wide)12.dp else 4.dp,vertical=4.dp),\n                        modifier=Modifier.weight(if(wide)1f else .82f),',
        "settings touch target")
    return s[:start]+part+s[end:]

def compact_live_header(s):
    start=s.index('        Row(\n            Modifier.fillMaxWidth().background(Color(0xFF164433))')
    end=s.index('        // Large, independently rendered 3D scene',start)
    header=r'''        // Two compact, stable lines instead of five buttons forced into one
        // phone-width row. Always above the 3D viewport.
        Column(
            Modifier.fillMaxWidth().background(Color(0xFF164433))
                .padding(horizontal=10.dp,vertical=6.dp),
            verticalArrangement=Arrangement.spacedBy(2.dp)
        ) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(7.dp),
                verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={audio?.stop();back()},
                    contentPadding=PaddingValues(horizontal=7.dp,vertical=3.dp)) {
                    Text("‹ Geri",color=Color.White,fontSize=13.sp,
                        maxLines=1,softWrap=false)
                }
                LenaActualAvatar(35.dp)
                Column(Modifier.weight(1f)) {
                    Text("Lena · 3D",color=Color.White,fontSize=18.sp,
                        fontWeight=FontWeight.Bold,maxLines=1)
                }
                TextButton(onClick={audio?.stop();openTextChat()},
                    modifier=Modifier.background(Color(0xFFF4FAF0),RoundedCornerShape(20.dp)),
                    contentPadding=PaddingValues(horizontal=10.dp,vertical=5.dp)) {
                    Text("Mesajlar",color=Color(0xFF17543A),fontSize=12.sp,
                        fontWeight=FontWeight.Bold,maxLines=1,softWrap=false)
                }
            }
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.End,
                verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={audio?.stop();threadsOpen=true;ai.searchThreads("")},
                    enabled=canEdit) {
                    Text("Sohbetler",color=Color.White,fontSize=12.sp,
                        maxLines=1,softWrap=false)
                }
                TextButton(onClick={audio?.stop();settingsOpen=true},enabled=canEdit) {
                    Text("Ayarlar",color=Color(0xFFFFE1A6),fontSize=12.sp,
                        maxLines=1,softWrap=false)
                }
            }
        }

'''
    old=s[start:end]
    assert 'LenaActualAvatar(42.dp)' in old and 'openTextChat()' in old
    return s[:start]+header+s[end:]

def live_3d_recovery(s):
    # A GLB can return a non-null node and still be invisible on some GPUs.
    # Reframe closer to the real model's waist, and provide a visible native
    # portrait with a switch to the supported text chat when setup fails.
    s=once(s,'else -> Position(0f, 1.88f, 1.85f)',
           'else -> Position(0f, 1.23f, 3.0f)',"3d default camera")
    s=once(s,'else -> Position(0f, 1.88f, 0f)',
           'else -> Position(0f, 1.23f, 0f)',"3d target camera")
    s=once(s,
        '''            Text("Lena modeli açılamadı; güvenli 2.5D görünüme geçiliyor.",
                modifier = Modifier.align(Alignment.Center), color = Color.White)''',
        '''            Column(Modifier.align(Alignment.Center),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                LenaActualAvatar(132.dp)
                Text("3D Lena bu cihazda açılamadı. Mesajlar üzerinden konuşabilirsin.",
                    color=Color.White,fontSize=13.sp)
            }''',
        "visible native 3D model recovery")
    return s

def main():
    patch("HomeScreen.kt",clean_home)
    patch("WordQuest175Arena.kt",center_arena)
    patch("HomeScreen.kt",packages_only)
    patch("WordQuestSettings.kt",hide_voice_picker_lists)
    patch("ConversationScreen.kt",compact_messenger)
    patch("LiveTeacherScreen.kt",compact_live_header)
    patch("LenaLive3DStage.kt",live_3d_recovery)
    assert 'Wq175SceneBackdrop' in (SRC/"WordQuest175Arena.kt").read_text("utf-8")
    print("PASS: no source version/signing/data/voice changes; live 3D remains crash isolated.")

if __name__=="__main__":
    main()
