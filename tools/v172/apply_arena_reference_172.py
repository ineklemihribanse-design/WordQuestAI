#!/usr/bin/env python3
"""Apply the user's Arena UI references to the cumulative v1.0.71 Android project.

Preserves backend modes, puzzle answers, scores, online/offline rules, Lena studio,
Word Memory, AI keys, signing identity and all persisted user records.
Run strictly AFTER the v171 installer, never against v1.0.69 main sources.
"""
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
APP = ROOT / "source/app"
TOOLS = ROOT / "tools/v172"

def exactly_one(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise AssertionError(f"{label}: expected one source anchor, got {count}")
    return text.replace(old, new, 1)

def patch_home():
    path = SRC / "ArenaApprovedHome.kt"
    code = path.read_text("utf-8")
    start = code.index('        ArenaApprovedPanel("Oyun Türü","🎮"){')
    end = code.index('        ArenaApprovedPanel("İngilizce Seviyesi","🎓"){', start)
    code = code[:start] + (
        '        // Selected mode feeds the original Arena multiplayer/offline engine.\n'
        '        Arena172AdventurePicker(mode, onMode, allowTeam = !solo)\n'
    ) + code[end:]
    code = exactly_one(
        code,
        '        ArenaApprovedPanel("İngilizce Seviyesi","🎓"){',
        '        ArenaApprovedPanel("Maç Kurulumu","⚙"){\n'
        '            Text("🎓  İngilizce Seviyesi",color=ArenaApproved.ink,\n'
        '                fontSize=13.sp,fontWeight=FontWeight.Bold)',
        "unified match setup"
    )
    code = exactly_one(
        code,
        '        }\n        ArenaApprovedPanel("Konu / Paket","▱"){',
        '            Text("▱  Konu / Paket",color=ArenaApproved.ink,\n'
        '                fontSize=13.sp,fontWeight=FontWeight.Bold)',
        "topic inside match setup"
    )
    start = code.index('        ArenaApprovedPanel("Maç Ayarları","⚙"){')
    end = code.index('        if(!solo) ArenaRulesCardV3(', start)
    code = code[:start] + (
        '        Arena172MatchSettings(mode, seconds, allowedTimes, onSeconds,\n'
        '            size, sizes, onSize)\n'
    ) + code[end:]
    # Keep native mode/level/topic/solo selections wired to the existing callbacks.
    for anchor in ('onLevel(value)', 'onTopic(value)', 'onOffline', 'onCreate',
                   'onJoin', 'Arena172AdventurePicker(', 'Arena172MatchSettings('):
        assert anchor in code, anchor
    path.write_text(code, "utf-8")

def patch_score():
    path = SRC / "ArenaV2Visuals.kt"
    code = path.read_text("utf-8")
    start = code.index('@Composable\ninternal fun ArenaLiveScoreV2(')
    end = code.index('@Composable\ninternal fun ArenaVictoryV2(', start)
    wrapper = '''@Composable
internal fun ArenaLiveScoreV2(
    hostName: String, guestName: String, hostScore: Int, guestScore: Int,
    isHostTurn: Boolean, countdown: Int?, completed: Int, total: Int, cooperative: Boolean,
    hostAvatar: String = "robot", guestAvatar: String = "fox"
) {
    Arena172LiveScoreboard(hostName,guestName,hostScore,guestScore,isHostTurn,
        countdown,completed,total,cooperative,hostAvatar,guestAvatar)
}

'''
    code = code[:start] + wrapper + code[end:]
    assert 'ArenaVictoryV2(' in code and 'ArenaRiggedMascot(' in code
    path.write_text(code, "utf-8")

def patch_actions(filename):
    path = SRC / filename
    code = path.read_text("utf-8")
    # The original v152 pass and full-word answer-validation callbacks stay intact.
    old='ArenaOutlineButton("PAS", {'
    if code.count(old) == 1:
        code = code.replace(old, 'ArenaOutlineButton("»  PAS GEÇ", {', 1)
    else:
        raise AssertionError(f"{filename}: PAS button anchor changed, found {code.count(old)}")
    patterns = [
        r'(ArenaGoldButton\(\s*)"KONTROL ET"',
        r'(ArenaGoldButton\(\s*)"CEVABI KONTROL ET"',
        r'(ArenaGoldButton\(\s*)"CEVABI DOĞRULA"',
        r'(ArenaGoldButton\(\s*)"CEVAPLA"',
        r'(ArenaGoldButton\(\s*)"KONTROL"',
    ]
    changed = 0
    for pattern in patterns:
        code, n = re.subn(pattern, lambda m: m.group(1) + '"✓  CEVABI GÖNDER"', code)
        changed += n
    # Do not silently claim the requested new submit label if the screen uses
    # a different call-site; stop CI for inspection rather than changing game rules.
    if changed == 0:
        raise AssertionError(f"{filename}: submit button call-site not recognized")
    if changed > 2:
        raise AssertionError(f"{filename}: ambiguous answer submit buttons: {changed}")
    assert 'crosswordFullWordMatches' in code
    path.write_text(code, "utf-8")
    print(filename, "answer labels updated:", changed)

def main():
    if not (SRC / "ArenaApprovedHome.kt").is_file() or not (SRC / "VocabularyNeonHome.kt").is_file():
        raise AssertionError("The cumulative Arena + Word Memory project must be assembled first.")
    gradle = APP / "build.gradle.kts"
    build = gradle.read_text("utf-8")
    assert build.count('versionCode = 96') == 1
    assert build.count('versionName = "1.0.71-lena-animation-chat-tools"') == 1
    assert 'applicationIdSuffix = ".fresh"' in build
    for important in ("ConversationScreen.kt", "LiveTeacherScreen.kt",
                      "VocabularyNeonHome.kt", "VocabularyNeonPractice.kt"):
        assert (SRC / important).is_file(),important
    shutil.copyfile(TOOLS / "ArenaReferenceComponents.kt", SRC / "ArenaReferenceComponents.kt")
    patch_home()
    patch_score()
    patch_actions("PremiumOnlineScreen.kt")
    patch_actions("PremiumArenaScreen.kt")
    build = build.replace('versionCode = 96','versionCode = 97',1)
    build = build.replace('versionName = "1.0.71-lena-animation-chat-tools"',
                          'versionName = "1.0.72-arena-reference-controls"',1)
    gradle.write_text(build,"utf-8")
    print("v1.0.72: three actual modes, compact game setup, settings and circular score timer.")
    print("PAS GEÇ and CEVABI GÖNDER remain hooked to the existing game logic.")
    print("Package/signing, Lena, Word Memory and persisted player data remain unchanged.")

if __name__ == "__main__":
    main()
