#!/usr/bin/env python3
"""Fail-closed v1.0.83 overlay. MUST run strictly after tools/v182/apply_ui_182.py.
Never resets SQLite, preferences, Firebase, installed package identity, art or mascots.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TOOLS = ROOT / "tools/v183"


def replace_one(text: str, old: str, new: str, description: str) -> str:
    found = text.count(old)
    if found != 1:
        raise AssertionError(f"{description}: expected one source anchor, found {found}")
    return text.replace(old, new, 1)


def protect_answers(text: str, needle: str, validity: str, description: str) -> str:
    """Wrap only the matching forEach block; preserve all original callbacks verbatim."""
    if text.count(needle) != 1:
        raise AssertionError(f"{description}: ambiguous option loop")
    start = text.index(needle)
    opening = text.index("{", start)
    depth = 0
    ending = None
    for n in range(opening, len(text)):
        if text[n] == "{":
            depth += 1
        elif text[n] == "}":
            depth -= 1
            if depth == 0:
                ending = n + 1
                break
    if ending is None:
        raise AssertionError(f"{description}: unterminated option loop")
    original = text[start:ending]
    return text[:start] + (
        f"if (Wq183QuizIsValid({validity})) {{\n"
        + original
        + "\n                        } else {\n"
        + "                            Wq183QuizUnavailableQuestion()\n"
        + "                        }"
    ) + text[ending:]


def update(path: str, action):
    dest = SRC / path
    old = dest.read_text(encoding="utf-8")
    new = action(old)
    if new == old:
        raise AssertionError(f"v183 patch made no change: {path}")
    dest.write_text(new, encoding="utf-8")
    print("PASS v183", path)


shutil.copyfile(TOOLS / "Wq183QuizQuality.kt", SRC / "Wq183QuizQuality.kt")
shutil.copyfile(TOOLS / "Wq183ArenaBackdrop.kt", SRC / "Wq183ArenaBackdrop.kt")
import sys
sys.path.insert(0, str(TOOLS))
from stage_arena_assets_183 import stage as stage_arena
stage_arena()

from stage_icons_183 import stage as stage_icons
stage_icons()
shutil.copyfile(TOOLS / "Wq183HighResIcons.kt", SRC / "Wq183HighResIcons.kt")

def icon_route(source):
    return replace_one(
        source,
        "    val cropped=remember(atlas,index){",
        """    if(Wq183HighResArtwork(name,modifier)) return true
    val cropped=remember(atlas,index){""",
        "prefer user-approved 512px icon, fall back to existing atlas")
update("Wq182Icons.kt", icon_route)

def online(source):
    source = replace_one(
        source,
        """Text(question.prompt, color = Color.White, fontSize = 21.sp,
                            lineHeight=28.sp, fontWeight = FontWeight.Bold)""",
        "Wq183QuizPrompt(question.prompt)",
        "visible high-contrast online question card",
    )
    source = replace_one(source,
        '''Wq181ArenaHeroStage(
            Modifier.matchParentSize(), active = match?.phase == "playing")''',
        '''Wq183ArenaBackdrop(
            Modifier.matchParentSize(), active = match?.phase == "playing")''',
        "responsive original user Arena image with safe v181 fallback")
    source = protect_answers(
        source,
        "question.options.forEachIndexed { n, option ->",
        "question.prompt, question.options, question.correct",
        "online five-choice invalid-answer guard",
    )
    return source

update("PremiumOnlineScreen.kt", online)

# The live Arena already uses a separately configured FirebaseApp. Expose the
# exact authenticated instance alongside its existing database; do NOT create
# a disconnected Firebase default app for mascot preview sharing.
update("Accounts.kt", lambda source: replace_one(
    source,
    '''    internal fun socialDatabase(): FirebaseDatabase? = database''',
    '''    internal fun socialDatabase(): FirebaseDatabase? = database
    internal fun socialAuth(): FirebaseAuth? = auth''',
    "reuse the room's authenticated Firebase instance for private previews"))

# Keep the existing score widgets and other Arena callbacks untouched.
# Earlier releases have several different scoreboard implementations, so the
# only safe stable insertion point is the common host/guest model binding.
# The compact portrait strip renders for all match phases, including results.
def online_two_player_portraits(source):
    anchor = '''    val guestName = ArenaNickname.safeDisplay(match.guestName, "Arkadaş bekleniyor")'''
    return replace_one(source, anchor,
        anchor + '''
    Wq183OnlinePortraitStrip(
        hostAvatar=match.hostAvatar, guestAvatar=match.guestAvatar,
        hostName=hostName, guestName=guestName,
        hostScore=match.score(match.hostUid, questions),
        guestScore=match.score(match.guestUid, questions),
        mode=match.mode, isHost=isHost
    )''',
        "render both participant maskots in online match + finished HUD")
update("PremiumOnlineScreen.kt", online_two_player_portraits)

update("PremiumOnlineScreen.kt", lambda source: replace_one(
    source,
    '''    val match = client.match
    LaunchedEffect(match?.code,match?.phase,match?.hostAvatar,match?.guestAvatar,''',
    '''    val match = client.match
    Wq183ArenaShareBridge(
        code=match?.code,
        hostUid=match?.hostUid,
        guestUid=match?.guestUid,
        hostAvatar=match?.hostAvatar,
        guestAvatar=match?.guestAvatar,
        profileKey=session.profileKey,
        auth=session.accounts.socialAuth(),
        database=session.accounts.socialDatabase()
    )
    LaunchedEffect(match?.code,match?.phase,match?.hostAvatar,match?.guestAvatar,''',
    "connect opponent preview to existing authenticated online Arena session"))

update("WordQuest175Arena.kt", lambda source: replace_one(
    source, 'Wq175SceneBackdrop("arena",Modifier.matchParentSize())',
    'Wq183ArenaBackdrop(Modifier.matchParentSize(),active=false)',
    "use consistent Arena art on premium Arena home"))

update(
    "PremiumArenaScreen.kt",
    lambda source: protect_answers(
        source,
        "current.options.forEachIndexed { index, option ->",
        "current.prompt, current.options, current.correct",
        "offline five-choice invalid-answer guard",
    ),
)

update(
    "Wq182QuizAndInfo.kt",
    lambda source: replace_one(
        source,
        "maxLines=4,overflow=TextOverflow.Ellipsis)",
        "softWrap=true)",
        "do not silently truncate lengthy five-choice answers",
    ),
)


# The existing v173 avatar preference, profile scope and defaults stay intact.
# Custom IDs are accepted only when installed under that same profile.
shutil.copyfile(TOOLS / "Wq183MascotStore.kt", SRC / "Wq183MascotStore.kt")
shutil.copyfile(TOOLS / "Wq183MascotManager.kt", SRC / "Wq183MascotManager.kt")
shutil.copyfile(TOOLS / "Wq183ArenaShare.kt", SRC / "Wq183ArenaShare.kt")
shutil.copyfile(TOOLS / "Wq183OnlinePortraitStrip.kt", SRC / "Wq183OnlinePortraitStrip.kt")

def mascots(source):
    source = replace_one(source,
        'fun valid(id:String?):Boolean=id!=null && id in ids',
        'fun valid(id:String?):Boolean=id!=null && (id in ids || Wq183MascotStore.isCustomId(id))',
        "allow custom identifiers without changing original IDs")
    source = replace_one(source,
        'fun label(id:String):String=labels.getOrElse(ids.indexOf(id)){"Piko"}',
        'fun label(id:String):String=if(Wq183MascotStore.isCustomId(id)) "Kişisel maskot" '
        'else labels.getOrElse(ids.indexOf(id)){"Piko"}',
        "label personal portrait")
    source = replace_one(source,
        '''        val saved=prefs(context).getString("user_"+profile,null)
        return if(valid(saved))saved!! else "piko"''',
        '''        Wq183MascotStore.activate(context,profile)
        val saved=prefs(context).getString("user_"+profile,null)
        return if(valid(saved) && (!Wq183MascotStore.isCustomId(saved) ||
            Wq183MascotStore.exists(context,profile,saved!!))) saved!! else "piko"''',
        "custom mascots are selected only from their owner profile")
    source = replace_one(source,
        '''        require(valid(id))
        prefs(context).edit().putString("user_"+profile,id).apply()''',
        '''        require(valid(id))
        if(Wq183MascotStore.isCustomId(id))
            require(Wq183MascotStore.exists(context,profile,id))
        Wq183MascotStore.activate(context,profile)
        prefs(context).edit().putString("user_"+profile,id).apply()''',
        "prevent cross-account selection")
    source = replace_one(source,
        '''        val safe=if(valid(id))id else "piko"
        val pose=if(victory)"victory" else "idle"''',
        '''        if(Wq183MascotStore.isCustomId(id)) {
            return Wq183MascotStore.picture(context,id,victory)
                ?: picture(context,"piko",victory)
        }
        val safe=if(valid(id))id else "piko"
        val pose=if(victory)"victory" else "idle"''',
        "draw selected local artwork, default portrait on opponent device")
    source = replace_one(source,
        '''    val bitmap=remember(context,id,victory,revision) {
        Wq2DMascots.picture(context,id,victory)
    }''',
        '''    // State-map read makes the rival preview reactive without altering saved art.
    val opponentPreview=Wq183ArenaShare.remoteImage(id)
    val bitmap=remember(context,id,victory,revision,opponentPreview) {
        opponentPreview ?: Wq2DMascots.picture(context,id,victory)
    }''',
        "render room-scoped remote custom preview")
    return source

update("WordQuest2DCharacters.kt", mascots)

def account_mascot(source):
    return replace_one(source,
        '''                TextButton(onClick={choosingMaskot=true}) {
                    Text("Karakteri değiştir",fontSize=13.sp)
                }''',
        '''                TextButton(onClick={choosingMaskot=true}) {
                    Text("Karakteri değiştir",fontSize=13.sp)
                }
                Wq183MascotManager(
                    profileKey=session.profileKey,
                    selected=chosenMaskot.value,
                    onlineEligible=session.accountName!=null,
                    onSelected={id->
                        Wq2DMascots.choose(appContext,session.profileKey,id)
                        chosenMaskot.value=id
                    }
                )''',
        "custom ZIP importer beside profile avatar")

update("HomeScreen.kt", account_mascot)

gradle = ROOT / "source/app/build.gradle.kts"
before = gradle.read_text("utf-8")
assert before.count("versionCode = 108") == 1
assert before.count('versionName = "1.0.82"') == 1
assert 'applicationIdSuffix = ".fresh"' in before
gradle.write_text(
    before.replace("versionCode = 108", "versionCode = 109", 1)
    .replace('versionName = "1.0.82"', 'versionName = "1.0.83"', 1),
    "utf-8",
)
print("PASS: 1.0.83 code109; existing application identity and saved-data paths unchanged.")
