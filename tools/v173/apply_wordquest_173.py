#!/usr/bin/env python3
"""v1.0.73 candidate: unified Arena match controls + user-source avatar previews.

Install after v172, preserving the 1.0.72 build and the existing AI/Lena/data
screens. Only preview images with confirmed CC-BY metadata are bundled;
3D assets and animations are NOT complete, so do not publish a final release.
"""
from pathlib import Path
import base64, hashlib, shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app"
PKG=ROOT/"tools/v173"

def once(code,old,new,label):
    count=code.count(old)
    if count!=1:
        raise AssertionError(f"{label}: expected 1 anchor, got {count}: {old[:75]!r}")
    return code.replace(old,new,1)

def patch_arena_home():
    p=SRC/"ArenaApprovedHome.kt"
    s=p.read_text("utf-8")
    s=once(s,'    soloInitially: Boolean = false\n','    soloInitially: Boolean = false,\n'
        '    avatarProfileKey:String=playerName\n',"profile-scoped avatar key")
    start=s.index('    var mascot by remember {')
    end=s.index('    var avatarPicker by rememberSaveable',start)
    s=s[:start]+'''    var mascot by remember(avatarProfileKey) {
        mutableStateOf(WordQuestAvatarStore.read(context,avatarProfileKey))
    }
    LaunchedEffect(avatarProfileKey,mascot) {
        // The legacy online room reads a single global preference; synchronize it
        // when the active profile changes, without changing other users' records.
        prefs.edit().putString("kind",mascot).apply()
    }
'''+s[end:]
    s=once(s,'ArenaMascotV2(mascot,Modifier.size(26.dp))',
        'WordQuestAvatarPortrait(mascot,Modifier.size(26.dp))',"profile preview")
    start=s.index('        // Mascot previews are display-only:')
    end=s.index('        Row(\n            Modifier.fillMaxWidth().height(152.dp)',start)
    gallery='''        // Show the ten user-supplied avatar sources; permission pending models
        // are labelled rather than replaced by invented cartoon animals.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            WordQuestAvatarStore.all.forEach { choice ->
                Column(Modifier.width(78.dp).clip(RoundedCornerShape(16.dp))
                    .background(if(mascot==choice.id)Color(0xFFEAF9E3)
                        else Color(0xFFF7F2FF))
                    .clickable { avatarPicker=true }
                    .padding(5.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    WordQuestAvatarPortrait(choice.id,Modifier.size(64.dp))
                    Text(choice.name,color=ArenaApproved.ink,fontSize=11.sp,
                        fontWeight=FontWeight.Bold)
                    if(!choice.licensedForBuild) Text("Lisans bekliyor",
                        color=ArenaApproved.sub,fontSize=8.sp,maxLines=1)
                }
            }
        }
'''
    s=s[:start]+gallery+s[end:]
    s=once(s,'ArenaApprovedHeroArt(kind=mascot,modifier=Modifier.weight(.98f).fillMaxHeight())',
        'WordQuestAvatarPortrait(mascot,Modifier.weight(.98f).fillMaxHeight())',
        "real-model hero preview")
    s=once(s,'        Arena172AdventurePicker(mode, onMode, allowTeam = !solo)',
        '        Arena173AdventurePicker(mode,onMode,solo)',
        "replace simple mode icons with shaded 3D-style art")
    start=s.index('        ArenaApprovedPanel("Maç Kurulumu","⚙"){')
    end=s.index('        if(!solo) ArenaRulesCardV3(',start)
    settings='''        Arena173UnifiedMatchSettings(
            solo=solo,onSolo={solo=it},mode=mode,onMode=onMode,
            level=level,onLevel=onLevel,topic=topic,topics=topics,
            onTopic=onTopic,seconds=seconds,allowedTimes=allowedTimes,
            onSeconds=onSeconds,size=size,sizes=sizes,onSize=onSize
        )
'''
    s=s[:start]+settings+s[end:]
    start=s.index('    if(avatarPicker){')
    s=s[:start]+'''    if(avatarPicker) WordQuestSharedAvatarPicker(
        selected=mascot,
        onSelect={ chosen ->
            WordQuestAvatarStore.save(context,avatarProfileKey,chosen)
            mascot=chosen
        },
        onDismiss={avatarPicker=false}
    )
}
'''
    assert 'Arena173UnifiedMatchSettings(' in s and 'Arena173AdventurePicker(' in s
    assert s.count('ArenaApprovedPanel("Maç Kurulumu"')==0
    p.write_text(s,"utf-8")

def patch_arena_player_calls():
    for name in ("PremiumArenaScreen.kt","PremiumOnlineScreen.kt"):
        p=SRC/name
        s=p.read_text("utf-8")
        # The approved Arena home was already substituted in the v150 pass.
        token="ArenaApprovedHome(\n"
        s=once(s,token,token+'                    avatarProfileKey=session.profileKey,\n',
            "wire match screen profile to shared avatar")
        p.write_text(s,"utf-8")

def patch_rig_and_score():
    p=SRC/"ArenaMascotRig.kt"
    s=p.read_text("utf-8")
    s=once(s,'fun isValid(id: String?): Boolean = id != null && id in ids',
        'fun isValid(id: String?): Boolean = id != null && '
        '(id in ids || WordQuestAvatarStore.valid(id))',"accept model ids in rooms")
    p.write_text(s,"utf-8")
    p=SRC/"ArenaV2Visuals.kt"
    s=p.read_text("utf-8")
    s=once(s,'    ArenaRiggedMascot(kind,modifier)',
        '    if(WordQuestAvatarStore.valid(kind)) WordQuestAvatarPortrait(kind,modifier)\n'
        '    else ArenaRiggedMascot(kind,modifier)',
        "keep the user's model preview consistent in score panels")
    needle='ArenaRiggedMascot(winnerAvatar,Modifier.size(86.dp).scale(jump.value),holdingTrophy=!isDraw)'
    s=once(s,needle,
        '''if(WordQuestAvatarStore.valid(winnerAvatar))
                WordQuestAvatarPortrait(winnerAvatar,Modifier.size(86.dp).scale(jump.value))
            else ArenaRiggedMascot(winnerAvatar,Modifier.size(86.dp).scale(jump.value),
                holdingTrophy=!isDraw)''',
        "avoid falsely displaying robot as a selected user model")
    p.write_text(s,"utf-8")

def patch_memory():
    p=SRC/"VocabularyNeonHome.kt"
    s=p.read_text("utf-8")
    a=s.index('    var robot by remember(session.profileKey) {')
    b=s.index('    var robotPicker by remember',a)
    s=s[:a]+'''    var selectedAvatar by remember(session.profileKey) {
        mutableStateOf(WordQuestAvatarStore.read(context,session.profileKey))
    }
'''+s[b:]
    a=s.index('    if (robotPicker) {')
    b=s.index('    Box(Modifier.fillMaxSize().background(',a)
    s=s[:a]+'''    if(robotPicker) WordQuestSharedAvatarPicker(
        selected=selectedAvatar,
        onSelect={ chosen ->
            WordQuestAvatarStore.save(context,session.profileKey,chosen)
            selectedAvatar=chosen
        },
        onDismiss={robotPicker=false}
    )

'''+s[b:]
    s=once(s,'ProfileRobotPortrait(robot, Modifier.fillMaxSize())',
        '''Column(Modifier.fillMaxSize(),
                            horizontalAlignment=Alignment.CenterHorizontally) {
                            WordQuestAvatarPortrait(selectedAvatar,
                                Modifier.weight(1f).fillMaxWidth())
                            Text(WordQuestAvatarStore.model(selectedAvatar).name,
                                color=MemoryNavy,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        }''',
        "shared header profile")
    p.write_text(s,"utf-8")

    p=SRC/"VocabularyNeonPractice.kt"
    s=p.read_text("utf-8")
    s=once(s,
        '            Text("Kartlarla Çalış", color = DarkText, fontSize = 19.sp,',
        '''            WordQuestAvatarPortrait(
                WordQuestAvatarStore.read(LocalContext.current,session.profileKey),
                Modifier.size(39.dp))
            Text("Kartlarla Çalış", color = DarkText, fontSize = 19.sp,''',
        "study card avatar")
    s=once(s,'                Text("Çalışma tamamlandı", color = Color(0xFF167DCC),',
        '''                WordQuestAvatarPortrait(
                    WordQuestAvatarStore.read(LocalContext.current,session.profileKey),
                    Modifier.size(72.dp))
                Text("Çalışma tamamlandı", color = Color(0xFF167DCC),''',
        "study-complete preview")
    p.write_text(s,"utf-8")

def install_previews():
    b64=(PKG/"wq173_verified_previews.webp.b64").read_text("ascii").strip()
    decoded=base64.b64decode(b64,validate=True)
    assert len(decoded)==1748,len(decoded)
    assert hashlib.sha256(decoded).hexdigest()==(
        "792da177fafecddf20200e66e5f09c3f608cd73e9a453e0ce022096f9950c075"),(
        "Uploaded thumbnail content changed; refuse to build unverified images")
    assert decoded[:4]==b"RIFF" and decoded[8:12]==b"WEBP"
    out=APP/"src/main/assets/avatars/shared"
    out.mkdir(parents=True,exist_ok=True)
    (out/"wq173_verified_previews.webp").write_bytes(decoded)

def main():
    gradle=APP/"build.gradle.kts"
    old=gradle.read_text("utf-8")
    assert old.count('versionCode = 97')==1
    assert old.count('versionName = "1.0.72-arena-reference-controls"')==1
    assert 'applicationIdSuffix = ".fresh"' in old
    for needed in ("ArenaApprovedHome.kt","ArenaV2Visuals.kt",
                   "VocabularyNeonHome.kt","VocabularyNeonPractice.kt",
                   "ConversationScreen.kt","LenaLive3DStage.kt"):
        assert (SRC/needed).is_file(),needed
    for file in ("Arena173Components.kt","WordQuestSharedAvatars.kt"):
        shutil.copyfile(PKG/file,SRC/file)
    install_previews()
    patch_arena_home()
    patch_arena_player_calls()
    patch_rig_and_score()
    patch_memory()
    old=old.replace('versionCode = 97','versionCode = 98',1)
    old=old.replace('versionName = "1.0.72-arena-reference-controls"',
        'versionName = "1.0.73-arena-avatar-candidate"',1)
    gradle.write_text(old,"utf-8")
    print("Candidate v1.0.73: genuine-model previews + one shared selection,")
    print("three redesigned modes, one match panel with level/topic dropdowns.")
    print("NOT READY FOR RELEASE: four CC-BY model preview images only;")
    print("actual GLB rigging and commercial animal-pack rights remain open.")

if __name__=="__main__":
    main()
