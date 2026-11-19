#!/usr/bin/env python3
"""Attach five procedural 2.5D bone rigs to all Arena avatars and hero trophy.

Runs on top of v152's assembled Android sources. Replaces the old static 2D
draw function, leaving the app home screen, waiting room layout and game rules
unchanged. Only the mascots and trophy animation inside existing layouts change.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
MASCOTS=ROOT/"tools/v153"

def replace_one(text,old,new,label):
    count=text.count(old)
    if count!=1: raise RuntimeError(f"{label}: expected one anchor, found {count}")
    return text.replace(old,new,1)

def main():
    for name in ("ArenaMascotRig.kt","ArenaRiggedMascot.kt"):
        shutil.copyfile(MASCOTS/name,SRC/name)
    TEST.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(MASCOTS/"ArenaMascotRigTest.kt",TEST/"ArenaMascotRigTest.kt")

    # Public wrapper used by avatar cards, scoreboard and profile also becomes rigged.
    visual=SRC/"ArenaV2Visuals.kt"
    s=visual.read_text(encoding="utf-8")
    a=s.index("/** Small original vector mascots, not network images or a 3D engine. */")
    b=s.index("@Composable\ninternal fun ArenaLiveScoreV2(",a)
    replacement='''/** Render the shared five-character articulated 2.5D rig. */
@Composable
internal fun ArenaMascotV2(kind: String, modifier: Modifier = Modifier) {
    ArenaRiggedMascot(kind,modifier)
}

'''
    s=s[:a]+replacement+s[b:]
    s=replace_one(s,
        "ArenaMascotV2(winnerAvatar,Modifier.size(86.dp).scale(jump.value))",
        "ArenaRiggedMascot(winnerAvatar,Modifier.size(86.dp).scale(jump.value),holdingTrophy=!isDraw)",
        "victory character also holds trophy")
    visual.write_text(s,encoding="utf-8")

    # The old hero always showed a still robot regardless of selection.
    # Replace with the selected character's live articulated hand+trophy rig.
    theme=SRC/"ArenaApprovedTheme.kt"
    s=theme.read_text(encoding="utf-8")
    a=s.index("/** Drawn in Compose, so the approved hero mascot and trophy remain crisp on mobile. */")
    b=s.index("/** Reusable purple raised button like the approved lobby mockup. */",a)
    replacement='''/**
 * The hero is live rig animation, not a static robot crop. Exactly the same
 * selected character appears in the avatar chip, room, scoreboard and hero.
 */
@Composable
internal fun ArenaApprovedHeroArt(kind:String,modifier:Modifier=Modifier) {
    Box(modifier,contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width
            val h=size.height
            drawCircle(Color(0xFFE6D7FF).copy(alpha=.70f),
                radius=w*.35f,center=Offset(w*.73f,h*.7f))
            drawCircle(Color(0xFFDAC6FA).copy(alpha=.60f),
                radius=w*.22f,center=Offset(w*.3f,h*.9f))
            drawCircle(Color(0xFF99D956),radius=4.dp.toPx(),
                center=Offset(w*.22f,h*.28f))
            drawCircle(Color(0xFFFFCD45),radius=4.dp.toPx(),
                center=Offset(w*.90f,h*.13f))
        }
        ArenaRiggedMascot(kind,Modifier.fillMaxSize(),holdingTrophy=true)
    }
}

'''
    s=s[:a]+replacement+s[b:]
    theme.write_text(s,encoding="utf-8")

    # Backward compatible avatar IDs, exactly five character cards and an
    # endlessly scrollable selector on phones narrower than 400dp.
    home=SRC/"ArenaApprovedHome.kt"
    s=home.read_text(encoding="utf-8")
    s=replace_one(s,
        'var mascot by remember { mutableStateOf(prefs.getString("kind","robot")?:"robot") }',
        'var mascot by remember { mutableStateOf(ArenaMascotRig.safe(prefs.getString("kind","robot"))) }',
        "validate saved avatar preference")
    s=replace_one(s,
        'Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,\n'
        '            verticalAlignment=Alignment.CenterVertically) {\n'
        '            listOf("robot" to "Bip","fox" to "Pufi","owl" to "Mimo").forEach{(key,label)->',
        'Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),\n'
        '            horizontalArrangement=Arrangement.spacedBy(12.dp),\n'
        '            verticalAlignment=Alignment.CenterVertically) {\n'
        '            ArenaMascotRig.choices.forEach{(key,label)->',
        "five-character selectable showcase")
    s=replace_one(s,
        'Column(horizontalAlignment=Alignment.CenterHorizontally,\n'
        '                    verticalArrangement=Arrangement.spacedBy(3.dp)) {',
        'Column(Modifier.width(66.dp),horizontalAlignment=Alignment.CenterHorizontally,\n'
        '                    verticalArrangement=Arrangement.spacedBy(3.dp)) {',
        "compact scrollable profile display")
    # Distribute five characters across wider screens instead of clustering
    # them on the left. A narrow phone may scroll the same five choices.
    s=replace_one(s,
        'Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),\n'
        '            horizontalArrangement=Arrangement.spacedBy(12.dp),\n'
        '            verticalAlignment=Alignment.CenterVertically) {\n'
        '            ArenaMascotRig.choices.forEach{(key,label)->',
        'BoxWithConstraints(Modifier.fillMaxWidth()) {\n'
        '            val wideAvatars=maxWidth >= 420.dp\n'
        '            Row(if(wideAvatars) Modifier.fillMaxWidth()\n'
        '                else Modifier.horizontalScroll(rememberScrollState()),\n'
        '                horizontalArrangement=if(wideAvatars) Arrangement.SpaceEvenly\n'
        '                    else Arrangement.spacedBy(12.dp),\n'
        '                verticalAlignment=Alignment.CenterVertically) {\n'
        '            ArenaMascotRig.choices.forEach{(key,label)->',
        "responsive avatar row")
    s=replace_one(s,
        '        }\n        Row(\n            Modifier.fillMaxWidth().height(152.dp)',
        '        }\n        }\n        Row(\n            Modifier.fillMaxWidth().height(152.dp)',
        "close avatar bounds")
    # Each illustrated character must be selectable directly. Previously the
    # five-character row was decorative, confusing players who tapped it.
    s=replace_one(s,
        'Column(Modifier.width(66.dp),horizontalAlignment=Alignment.CenterHorizontally,',
        'Column(Modifier.width(66.dp).clickable {\n'
        '                        mascot=key\n'
        '                        prefs.edit().putString("kind",key).apply()\n'
        '                    },horizontalAlignment=Alignment.CenterHorizontally,',
        "tap avatar to update profile, hero and new online room")
    s=replace_one(s,
        'ArenaApprovedMascot(key,selected=mascot==key,sizeDp=64)',
        'ArenaApprovedMascot(key,selected=mascot==key,sizeDp=61)',
        "avatar preview dimensions")
    s=replace_one(s,
        'ArenaApprovedHeroArt(Modifier.weight(.98f).fillMaxHeight())',
        'ArenaApprovedHeroArt(kind=mascot,modifier=Modifier.weight(.98f).fillMaxHeight())',
        "hero uses chosen avatar")
    s=replace_one(s,
        'Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){\n'
        '                        listOf("robot" to "Bip","fox" to "Pufi","owl" to "Mimo")\n'
        '                            .forEach { (key,label) ->',
        'Row(Modifier.horizontalScroll(rememberScrollState()),\n'
        '                        horizontalArrangement=Arrangement.spacedBy(7.dp)){\n'
        '                        ArenaMascotRig.choices.forEach { (key,label) ->',
        "all five avatars inside picker")
    start=s.index('    if(avatarPicker){')
    end=s.index('            confirmButton=',start)
    picker=s[start:end]
    if picker.count("Modifier.weight(1f)")!=1:
        raise RuntimeError("Avatar picker layout anchor changed")
    s=s[:start]+picker.replace("Modifier.weight(1f)","Modifier.width(75.dp)")+s[end:]
    home.write_text(s,encoding="utf-8")

    # Online host/guest avatar values now accept Lila and Tiko as well.
    client=SRC/"PremiumOnlineRoom.kt"
    s=client.read_text(encoding="utf-8")
    s=replace_one(s,
        '?.takeIf {it in listOf("robot","fox","owl")} ?: "robot"',
        '?.takeIf {ArenaMascotRig.isValid(it)} ?: "robot"',
        "validate all five selected avatar IDs in online client")
    client.write_text(s,encoding="utf-8")
    print("Arena V153 rig: five selectable, full-bodied 2.5D articulated characters.")
    print("Hero trophy is attached to each chosen character wrist; lobby unchanged.")

if __name__=="__main__":
    main()
