#!/usr/bin/env python3
"""Install original 2D avatars on TOP of the signed 1.0.72 source assembly.

Keep all Kotlin, Lena, AI settings, word records, appId, signature, US/UK voices
and updater intact. No GLB/Filament avatar dependencies. versionCode 97 -> 98.
"""
from pathlib import Path
import shutil, base64, hashlib

ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/"tools/v173_2d"
APP=ROOT/"source/app"
SRC=APP/"src/main/java/com/kelimebulmacasi/a1"

def one(s,old,new,label):
    n=s.count(old)
    if n!=1:raise AssertionError(f"{label}: expected one anchor, found {n}")
    return s.replace(old,new,1)

def arena_home():
    p=SRC/"ArenaApprovedHome.kt"
    s=p.read_text("utf-8")
    s=one(s,'    soloInitially: Boolean = false\n',
        '    soloInitially: Boolean = false,\n'
        '    avatarProfileKey: String = playerName\n',"arena profile key")
    a=s.index('    var mascot by remember {')
    b=s.index('    var avatarPicker by rememberSaveable',a)
    s=s[:a]+'''    var mascot by rememberWq2DSelection(avatarProfileKey)
'''+s[b:]
    s=one(s,'ArenaMascotV2(mascot,Modifier.size(26.dp))',
        'Wq2DPortrait(mascot,Modifier.size(26.dp))',"arena profile portrait")
    a=s.index('        // Mascot previews are display-only:')
    b=s.index('        Row(\n            Modifier.fillMaxWidth().height(152.dp)',a)
    s=s[:a]+'''        // Single compact control replaces the permanent big avatar gallery.
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
            Wq2DCompactSelector(mascot,onClick={avatarPicker=true})
        }
'''+s[b:]
    if 'ArenaApprovedHeroArt(Modifier.weight(.98f).fillMaxHeight())' in s:
        s=one(s,'ArenaApprovedHeroArt(Modifier.weight(.98f).fillMaxHeight())',
            'Wq2DPortrait(mascot,Modifier.weight(.98f).fillMaxHeight())',
            "arena banner art")
    else:
        s=one(s,
            'ArenaApprovedHeroArt(kind=mascot,modifier=Modifier.weight(.98f).fillMaxHeight())',
            'Wq2DPortrait(mascot,Modifier.weight(.98f).fillMaxHeight())',
            "arena banner art named")
    s=one(s,'        Arena172AdventurePicker(mode, onMode, allowTeam = !solo)',
        '        Arena173AdventurePicker(mode,onMode,solo)',
        "three professional game modes")
    a=s.index('        ArenaApprovedPanel("Maç Kurulumu","⚙"){')
    b=s.index('        if(!solo) ArenaRulesCardV3(',a)
    s=s[:a]+'''        Arena173UnifiedMatchSettings(
            solo=solo,onSolo={solo=it},mode=mode,onMode=onMode,
            level=level,onLevel=onLevel,topic=topic,topics=topics,
            onTopic=onTopic,seconds=seconds,allowedTimes=allowedTimes,
            onSeconds=onSeconds,size=size,sizes=sizes,onSize=onSize
        )
'''+s[b:]
    a=s.index('    if(avatarPicker){')
    s=s[:a]+'''    if(avatarPicker) Wq2DCharacterPicker(
        selected=mascot,
        onSelected={ id ->
            Wq2DMascots.choose(context,avatarProfileKey,id)
            mascot=id
        },
        onDismiss={avatarPicker=false}
    )
}
'''
    assert 'Arena173UnifiedMatchSettings(' in s
    assert 'Wq2DCompactSelector(' in s
    assert 'Arena172MatchSettings(' not in s
    p.write_text(s,"utf-8")

def arena_player_calls():
    for name in ("PremiumArenaScreen.kt","PremiumOnlineScreen.kt"):
        p=SRC/name
        s=p.read_text("utf-8")
        needle='ArenaApprovedHome(\n'
        s=one(s,needle,needle+
            '                    avatarProfileKey=session.profileKey,\n',
            "profile-scoped Arena "+name)
        p.write_text(s,"utf-8")

def arena_score_and_victory():
    p=SRC/"ArenaMascotRig.kt"
    s=p.read_text("utf-8")
    s=one(s,'fun isValid(id: String?): Boolean = id != null && id in ids',
        'fun isValid(id: String?): Boolean = id != null && '
        '(id in ids || Wq2DMascots.valid(id))',
        "allow original 2D ids in saved Arena room states")
    p.write_text(s,"utf-8")

    p=SRC/"ArenaV2Visuals.kt"
    s=p.read_text("utf-8")
    s=one(s,'    ArenaRiggedMascot(kind,modifier)',
        '    if(Wq2DMascots.valid(kind)) Wq2DPortrait(kind,modifier)\n'
        '    else ArenaRiggedMascot(kind,modifier)',
        "same selected avatar on match scoreboard")
    needle='ArenaRiggedMascot(winnerAvatar,Modifier.size(86.dp).scale(jump.value),holdingTrophy=!isDraw)'
    replacement='''if(Wq2DMascots.valid(winnerAvatar))
                Wq2DPortrait(winnerAvatar,
                    Modifier.size(110.dp).scale(jump.value),victory=!isDraw)
            else ArenaRiggedMascot(winnerAvatar,
                Modifier.size(86.dp).scale(jump.value),holdingTrophy=!isDraw)'''
    s=one(s,needle,replacement,"winning pose animation")
    p.write_text(s,"utf-8")

def memory_home():
    p=SRC/"VocabularyNeonHome.kt"
    s=p.read_text("utf-8")
    a=s.index('    var robot by remember(session.profileKey) {')
    b=s.index('    var robotPicker by remember',a)
    s=s[:a]+'''    var selectedAvatar by rememberWq2DSelection(session.profileKey)
'''+s[b:]
    a=s.index('    if (robotPicker) {')
    b=s.index('    Box(Modifier.fillMaxSize().background(',a)
    s=s[:a]+'''    if(robotPicker) Wq2DCharacterPicker(
        selected=selectedAvatar,
        onSelected={ id ->
            Wq2DMascots.choose(context,session.profileKey,id)
            selectedAvatar=id
        },
        onDismiss={robotPicker=false}
    )

'''+s[b:]
    old='''                    Box(Modifier.size(width = if (isCompact) 146.dp else 205.dp,
                        height = if (isCompact) 160.dp else 205.dp)
                        .clickable { robotPicker = true }) {
                        ProfileRobotPortrait(robot, Modifier.fillMaxSize())
                    }'''
    new='''                    // Compact selected character, not a permanent 205dp gallery.
                    Wq2DCompactSelector(selectedAvatar,
                        onClick={robotPicker=true})'''
    s=one(s,old,new,"compact Word Memory header")
    assert 'ProfileRobotPortrait(robot, Modifier.fillMaxSize())' not in s
    p.write_text(s,"utf-8")

def memory_practice():
    p=SRC/"VocabularyNeonPractice.kt"
    s=p.read_text("utf-8")
    s=one(s,
        '            Text("Kartlarla Çalış", color = DarkText, fontSize = 19.sp,',
        '''            Wq2DPortrait(
                Wq2DMascots.selected(LocalContext.current,session.profileKey),
                Modifier.size(38.dp))
            Text("Kartlarla Çalış", color = DarkText, fontSize = 19.sp,''',
        "study card selected avatar")
    s=one(s,'                Text("Çalışma tamamlandı", color = Color(0xFF167DCC),',
        '''                Wq2DPortrait(
                    Wq2DMascots.selected(LocalContext.current,session.profileKey),
                    Modifier.size(120.dp),victory=true)
                Text("Çalışma tamamlandı", color = Color(0xFF167DCC),''',
        "study victory pose")
    anchor='        if (studyMode == "card") {\n            if (!flipped && !reviewed) {'
    note='''        if (reviewed && reviews[position] == true && studyMode != "card") {
            Row(Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFE7F8EB)).padding(8.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Wq2DPortrait(
                    Wq2DMascots.selected(LocalContext.current,session.profileKey),
                    Modifier.size(44.dp))
                Text("Harika, doğru cevap!",color=Color(0xFF237146),
                    fontSize=12.sp,fontWeight=FontWeight.Bold)
            }
        }
'''
    s=one(s,anchor,note+anchor,"correct response uses selected avatar")
    p.write_text(s,"utf-8")

def icon_atlas():
    raw=base64.b64decode((PKG/"wq173_mode_icons.webp.b64").read_text("ascii").strip(),
        validate=True)
    assert len(raw)==4230
    assert hashlib.sha256(raw).hexdigest()==(
        "18d2f4a2782b4f7235f7c083bd2d41e83b79b5814d22a6d1557265904dcd84fb")
    assert raw[:4]==b"RIFF" and raw[8:12]==b"WEBP"
    target=APP/"src/main/assets/arena/icons"
    target.mkdir(parents=True,exist_ok=True)
    (target/"wq173_mode_icons.webp").write_bytes(raw)

def install_original_2d_art():
    # Exact original, user-approved images created for WordQuest with image generation.
    # Each atlas is 1120x448 pixels (five columns, two rows, 224px per pose).
    atlases={
        "idle":(218222,
            "00bc228719d1869d81ad16e1adab96991fa3e7a3c4bdcc00f2057b695e061253"),
        "victory":(239740,
            "f1701ece95b98fb82261e9c736bd2cc6d93b3246f07cffbc1261ed47f1217741"),
    }
    dest=APP/"src/main/assets/avatars/wordquest_2d"
    dest.mkdir(parents=True,exist_ok=True)
    for pose,(length,digest) in atlases.items():
        path=PKG/("wordquest_"+pose+"_atlas.webp.b64")
        raw=base64.b64decode(path.read_text("ascii").strip(),validate=True)
        assert len(raw)==length,(pose,len(raw))
        assert hashlib.sha256(raw).hexdigest()==digest,pose
        assert raw[:4]==b"RIFF" and raw[8:12]==b"WEBP",pose
        (dest/("wordquest_"+pose+"_atlas.webp")).write_bytes(raw)
    print("Embedded original 20 AI-generated character poses: idle and victory.")

def main():
    gradle=APP/"build.gradle.kts"
    s=gradle.read_text("utf-8")
    assert s.count('versionCode = 97')==1
    assert s.count('versionName = "1.0.72-arena-reference-controls"')==1
    assert 'applicationIdSuffix = ".fresh"' in s
    for name in ("LenaLive3DStage.kt","ConversationScreen.kt",
                 "ArenaApprovedHome.kt","VocabularyNeonHome.kt"):
        assert (SRC/name).exists(),name
    for name in ("Arena173Components.kt","WordQuest2DCharacters.kt",
                 "WordQuest2DPicker.kt"):
        shutil.copyfile(PKG/name,SRC/name)
    icon_atlas()
    install_original_2d_art()
    arena_home()
    arena_player_calls()
    arena_score_and_victory()
    memory_home()
    memory_practice()
    s=s.replace('versionCode = 97','versionCode = 99',1)
    s=s.replace('versionName = "1.0.72-arena-reference-controls"',
        'versionName = "1.0.73-original-2d-complete"',1)
    gradle.write_text(s,"utf-8")
    print("WordQuest AI 1.0.73 completed art hotfix code99: 20 bundled 2D images.")
    print("Same applicationIdSuffix .fresh and signing credentials as 1.0.72.")
    print("All 20 original 2D images are INCLUDED in the signed APK; no ZIP import needed.")

if __name__=="__main__":
    main()
