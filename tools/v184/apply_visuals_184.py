#!/usr/bin/env python3
"""Apply WordQuest AI 1.0.84 visual fixes AFTER the 1.0.83 overlay.

Input: repository checkout containing reconstructed source/ and the original,
SHA-pinned user Arena ZIP at assets/user_uploads/. Fail closed on drift.
No Firebase/SQLite/preferences changes, no clearing of existing mascot packs.
"""
from pathlib import Path
import hashlib, json, shutil, sys

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TOOLS = ROOT / "tools/v184"
ARENA = ROOT / "assets/user_uploads/WordQuestAI_Arena_13inc_Tablet_ve_Telefon.zip"
EXPECTED_ARENA = "80da38acbccf202c68f2f3b84acad93bfbcd9091554c125a26ff4808004b1906"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"1.0.84 {label}: expected 1 source anchor, found {count}")
    return text.replace(old, new, 1)


PENDING = {}

def edit(name, updates):
    file = SRC / name
    original = PENDING.get(file, file.read_text("utf-8"))
    modified = original
    for label, old, new in updates:
        modified = replace_once(modified, old, new, f"{name}: {label}")
    PENDING[file] = modified
    print(f"PASS 1.0.84 {name}: patch anchors verified")


# Exact SHA-256 of the user's approved, unmodified WebP bytes.
APPROVED_WEBP = {
    "arena_telefon_9x19_5.webp": "cb9cfce13f461d57d459acc7ea5b025ef338b89c4d89a986dafea9adc46ad486",
    "arena_telefon_9x19_5_koyu.webp": "42749638c831d9aa917e4e985b7859b00331113163ac4eebd88301ac9baf943c",
    "arena_tablet_13_16x10.webp": "fe3022d97bcedcc1eb9694cbf4e03eba662304b686faf9fe0eccf25d5bb58218",
    "arena_tablet_13_16x10_koyu.webp": "306c21ecc362d356ffb3493cd730ffe2341f3e82a0c6b25ff6fb805974d8efb7",
}

def stage_approved_arena():
    dest = ROOT / "source/app/src/main/assets/arena_v183"
    if ARENA.is_file():
        digest = hashlib.sha256(ARENA.read_bytes()).hexdigest()
        if digest != EXPECTED_ARENA:
            raise RuntimeError("Arena ZIP differs from approved original")
        # Backwards-compatible with original user ZIP, full v183 security checks.
        sys.path.insert(0, str(ROOT / "tools/v183"))
        from stage_arena_assets_183 import stage
        if not stage():
            raise RuntimeError("Arena original asset staging failed")
    else:
        # Four exact original WebPs are also committed in the v184 branch.
        # No need to store/upload the source ZIP to build the app.
        source = ROOT / "assets/user_uploads/arena_v184"
        for name, sha in APPROVED_WEBP.items():
            file = source / name
            if not file.is_file():
                raise RuntimeError(f"Approved Arena art missing: {name}")
            data = file.read_bytes()
            if hashlib.sha256(data).hexdigest() != sha:
                raise RuntimeError(f"Arena WebP bytes changed: {name}")
            if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
                raise RuntimeError(f"Invalid original Arena WebP: {name}")
            dest.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(file, dest / name)
    assert all((dest / name).is_file() and
        hashlib.sha256((dest / name).read_bytes()).hexdigest() == sha
        for name, sha in APPROVED_WEBP.items()), "Arena files changed after staging"
    print("PASS 1.0.84 original Arena tablet/phone scenes installed")


def apply():
    assert SRC.is_dir(), "Run AFTER restoring source and applying 1.0.83"
    stage_approved_arena()
    assert (TOOLS / "Wq184BitmapTrim.kt").is_file(), "Renderer helper missing"

    edit("Wq183MascotStore.kt", [
        ("crop custom art at render time without modifying files",
         '''            BitmapFactory.decodeFile(target.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = 2
            })''',
         '''            val decoded = BitmapFactory.decodeFile(
                target.absolutePath, BitmapFactory.Options().apply { inSampleSize = 1 }
            ) ?: return@runCatching null
            Wq184BitmapTrim.crop(decoded)'''),
        ("expose actual win pose presence scoped by profile",
         '''    private fun storedName(dir: File): String = runCatching {''',
         '''    fun hasWin(context: Context, profile: String, id: String): Boolean {
        if (!exists(context, profile, id)) return false
        val dir = File(root(context, profile), id)
        return runCatching {
            val json = JSONObject(File(dir, "manifest.json").readText(Charsets.UTF_8))
            val relative = json.getJSONObject("images").optString("win")
            pathPattern.matches(relative) && relative.startsWith("images/win.") &&
                File(dir, relative).isFile
        }.getOrDefault(false)
    }

    private fun storedName(dir: File): String = runCatching {'''),
    ])

    edit("Wq183MascotManager.kt", [
        ("larger local portrait preview",
         'Wq2DPortrait(mascot.id, Modifier.size(64.dp))',
         'Wq2DPortrait(mascot.id, Modifier.width(92.dp).height(142.dp))'),
        ("show actual uploaded victory image and warn when absent",
         '''                        OutlinedTextField(
                            value = draftName,''',
         '''                        if (Wq183MascotStore.hasWin(context, profileKey, mascot.id)) {
                            Text("Kupa pozu mevcut", style = MaterialTheme.typography.bodySmall)
                            Wq2DPortrait(mascot.id,
                                Modifier.fillMaxWidth().height(205.dp), victory = true)
                        } else {
                            Text("Kupa pozu yok: normal görsel kullanılır",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedTextField(
                            value = draftName,''')
    ])

    edit("WordQuest2DPicker.kt", [
        ("large real victory preview",
         '''                    Wq2DPortrait(selected,
                        Modifier.fillMaxWidth().height(125.dp),victory=true)''',
         '''                    Wq2DPortrait(selected,
                        Modifier.fillMaxWidth().height(265.dp),victory=true)''')
    ])

    edit("WordQuest174Memory.kt", [
        ("expand memory hero only for personal artwork",
         '''        .height(if(isCompact)250.dp else 295.dp)''',
         '''        .height(if (Wq183MascotStore.isCustomId(selected)) {
            if (isCompact) 340.dp else 418.dp
        } else if (isCompact) 250.dp else 295.dp)'''),
        ("portrait size follows full-body aspect instead of square fit",
         '''        Wq2DPortrait(selected,
            Modifier.align(Alignment.BottomEnd)
                .padding(end=6.dp,bottom=5.dp)
                .size(if(compact)163.dp else 220.dp))''',
         '''        Wq2DPortrait(selected,
            Modifier.align(Alignment.BottomEnd)
                .padding(end=6.dp,bottom=5.dp)
                .width(if (Wq183MascotStore.isCustomId(selected)) {
                    if (compact) 190.dp else 290.dp
                } else if (compact) 163.dp else 220.dp)
                .height(if (Wq183MascotStore.isCustomId(selected)) {
                    if (compact) 324.dp else 395.dp
                } else if (compact) 163.dp else 220.dp))'''),
    ])

    # The v175 phone/tablet polish replaced the original 258dp hero with a
    # BoxWithConstraints stage. Patch the *post-v175* layout, not obsolete code.
    # These anchors match source produced by apply_phone_tablet_polish_175.py
    # after the v183 background switch to Wq183ArenaBackdrop.
    edit("WordQuest175Arena.kt", [
        ("enlarge existing responsive stadium for personal artwork",
         '''            val heroHeight = if(compact)310.dp else if(wideTablet)378.dp else 342.dp''',
         '''            val customMascot = Wq183MascotStore.isCustomId(selectedAvatar)
            val heroHeight = if (customMascot) {
                if (compact) 382.dp else if (wideTablet) 438.dp else 405.dp
            } else if(compact)310.dp else if(wideTablet)378.dp else 342.dp
            val mascotWidth = if (customMascot) {
                if (compact) 212.dp else if (wideTablet) 288.dp else 262.dp
            } else if(compact)170.dp else if(wideTablet)218.dp else 196.dp
            val mascotHeight = if (customMascot) {
                if (compact) 348.dp else if (wideTablet) 410.dp else 380.dp
            } else mascotWidth'''),
        ("preserve stage but render tall personal mascot at full size",
         '''                        .offset(y=(-43).dp)
                        .size(if(compact)170.dp else if(wideTablet)218.dp else 196.dp),''',
         '''                        .offset(y=if (customMascot) (-24).dp else (-43).dp)
                        .width(mascotWidth).height(mascotHeight),'''),
    ])

    edit("Wq183OnlinePortraitStrip.kt", [
        ("pass game-finished status to portrait strip",
         '''    mode: String,
    isHost: Boolean
)''',
         '''    mode: String,
    isHost: Boolean,
    finished: Boolean = false
)'''),
        ("set victory pose for winner (both teammates in team mode)",
         '''        Wq183PlayerPortrait(hostAvatar, hostName, hostScore, isHost, Modifier.weight(1f))''',
         '''        Wq183PlayerPortrait(hostAvatar, hostName, hostScore, isHost,
            finished && (mode == "team" || hostScore > guestScore), Modifier.weight(1f))'''),
        ("victory pose for guest",
         '''        Wq183PlayerPortrait(guestAvatar, guestName, guestScore, !isHost, Modifier.weight(1f))''',
         '''        Wq183PlayerPortrait(guestAvatar, guestName, guestScore, !isHost,
            finished && (mode == "team" || guestScore > hostScore), Modifier.weight(1f))'''),
        ("accept victory and use slightly larger portraits",
         '''    isMe: Boolean,
    modifier: Modifier = Modifier
)''',
         '''    isMe: Boolean,
    victory: Boolean,
    modifier: Modifier = Modifier
)'''),
        ("winner image on online match result",
         '''        Wq2DPortrait(safeId, Modifier.size(42.dp))''',
         '''        Wq2DPortrait(safeId, Modifier.width(50.dp).height(72.dp), victory=victory)'''),
    ])

    edit("PremiumOnlineScreen.kt", [
        ("flag result state for correct winner artwork",
         '''        mode=match.mode, isHost=isHost
    )''',
         '''        mode=match.mode, isHost=isHost,
        finished=match.phase == "finished"
    )'''),
    ])

    # Fail closed if version is wrong; require explicit code 110/name 1.0.84.
    gradle = ROOT / "source/app/build.gradle.kts"
    build = gradle.read_text("utf-8")
    build = replace_once(build, "versionCode = 109", "versionCode = 110", "versionCode")
    build = replace_once(build, 'versionName = "1.0.83"',
                         'versionName = "1.0.84"', "versionName")
    PENDING[gradle] = build
    # All anchors validated before updating any source or Gradle text.
    for file, value in PENDING.items():
        file.write_text(value, "utf-8")
    shutil.copyfile(TOOLS / "Wq184BitmapTrim.kt", SRC / "Wq184BitmapTrim.kt")
    print("PASS v1.0.84 code 110, original package identity unchanged")
    print("PASS source overlay; next: real Kotlin compile, emulator, in-place update QA")


if __name__ == "__main__":
    apply()
