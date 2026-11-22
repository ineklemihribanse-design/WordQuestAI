#!/usr/bin/env python3
"""WordQuest AI 1.0.86 candidate overlay AFTER v1.0.85.

Requires exact verified v185 source. Stops on unexpected anchors, never touches
Firebase rules, personal mascot archives, Android applicationId, or user files.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
SRC_TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v186"

def exact_replace(text,old,new,label):
    count=text.count(old)
    assert count==1, f"{label}: expected exactly one source anchor, got {count}"
    return text.replace(old,new,1)

def apply():
    assert (SRC/"Wq185FriendMascot.kt").is_file(), "Run 1.0.85 overlay first"
    assert (SRC/"Wq184BitmapTrim.kt").is_file(), "Run 1.0.84 overlay first"
    gradle=ROOT/"source/app/build.gradle.kts"
    version=gradle.read_text("utf-8")
    assert version.count("versionCode = 111")==1
    assert version.count('versionName = "1.0.85"')==1
    assert 'applicationIdSuffix = ".fresh"' in version

    cards=SRC/"Wq180FriendCards.kt"
    s=cards.read_text("utf-8")
    assert "entry.avatarPreview" in s, "v185 friend preview missing"
    s=exact_replace(s,
        "    onMinor:(()->Unit)?=null\n){",
        "    onMinor:(()->Unit)?=null,\n    onProfile:(()->Unit)?=null\n){",
        "friend tile profile action")
    s=exact_replace(s,
        "if(mainButton!=null && onMain!=null || minorButton!=null && onMinor!=null) {",
        "if(mainButton!=null && onMain!=null || minorButton!=null && onMinor!=null || onProfile!=null) {",
        "profile action row visibility")
    s=exact_replace(s,
        "                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){",
        "                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){\n"
        "                        if(onProfile!=null)\n"
        "                            Wq180SmallAction(\"Profili incele\",onProfile,outlined=true)",
        "profile action label")
    s=exact_replace(s,
        "    var lookup by remember{mutableStateOf(\"\")}",
        "    var lookup by remember{mutableStateOf(\"\")}\n"
        "    var inspectedUid by remember{mutableStateOf<String?>(null)}",
        "profile sheet selected friend ID")
    s=exact_replace(s,
        "                    onMinor=if(canInvite)null else { {hub.removeFriend(p.uid)} })",
        "                    onMinor=if(canInvite)null else { {hub.removeFriend(p.uid)} },\n"
        "                    onProfile={inspectedUid=p.uid})",
        "show profile on mutual friend's card only")
    assert s.rstrip().endswith("}\n}"), "friend panel ending changed unexpectedly"
    s=s.rstrip()[:-1]+'''
    // Read live friend state; closing the friendship immediately hides details.
    val friendProfile = hub.friends.firstOrNull { it.uid==inspectedUid }
    if(friendProfile!=null) Wq186FriendProfileDetail(friendProfile){
        inspectedUid=null
    }
}
'''
    assert s.count("onProfile={inspectedUid=p.uid}")==1
    cards.write_text(s,"utf-8")

    for source,destination in (
      ("Wq186ArenaGridPolicy.kt", "Wq186ArenaGridPolicy.kt"),
      ("ArenaBoardV186.kt","ArenaBoardV2.kt"),
      ("Wq186FriendProfileDetail.kt","Wq186FriendProfileDetail.kt"),
    ):
        assert (PKG/source).is_file()
        shutil.copyfile(PKG/source, SRC/destination)
    SRC_TEST.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(PKG/"Wq186ArenaGridPolicyTest.kt",
                    SRC_TEST/"Wq186ArenaGridPolicyTest.kt")
    version=version.replace("versionCode = 111","versionCode = 112",1)
    version=version.replace('versionName = "1.0.85"','versionName = "1.0.86"',1)
    gradle.write_text(version,"utf-8")
    print("PASS: 1.0.86 board IME viewport, larger mutually-shared friend profile; v185 cumulative, no Firebase mutation")

if __name__=="__main__":
    apply()
