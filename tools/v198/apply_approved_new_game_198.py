#!/usr/bin/env python3
"""Apply the approved New Game art and responsive layout after v197.
Do not modify PuzzleScreen, the board, navigation outside New Game or game logic.
"""
import base64
import hashlib
import json
import re
import shutil
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
SCREEN = SRC / "NewGameScreen.kt"
UI = SRC / "Wq197CompactNewGameUi.kt"
ASSETS = ROOT / "source/app/src/main/assets/new_game_198"

EXPECTED = {
    "level": "5c1097778fce98344e6ba348830033c5580cf181ebb243ba422b365f344cf176",
    "topic": "baa1de16be9dcc6f924935548161060efc2f1646c07f9baea40314389106d43f",
    "kind": "27d5585bb249d927d6d7f70970c477b00c79750c16738085dca43bce35f119c9",
    "settings": "57659cf872b900ce470113dc5c17dc5a316d51c08adb07588c1f744316c7a0de",
    "phone": "d7cc96e8f3dbb73087820e8b2247df0a2b1f252ecaa7e527741ae4f212e0561f",
    "tablet": "11bfb7a2535b3f925f114e262119299758894fd3d9dad5c37e909b839407cc9b",
}


def one(s, old, new):
    assert s.count(old) == 1, (old[:150], s.count(old))
    return s.replace(old, new, 1)


def read_art():
    encoded = json.loads((HERE / "new_game_artwork.b64.json").read_text())
    assert set(encoded) == set(EXPECTED)
    output = {}
    for name, value in encoded.items():
        data = base64.b64decode(value, validate=True)
        assert data[:4] == b"RIFF" and data[8:12] == b"WEBP", name
        assert hashlib.sha256(data).hexdigest() == EXPECTED[name], name
        output[name] = data
    return output


def remove_call(s, name):
    match = re.search(r"(?m)^        " + name + r"\(", s)
    if not match:
        return s
    # This purely visual summary may have several arguments or nested calls.
    start, i, depth = match.start(), match.end(), 1
    while depth and i < len(s):
        if s[i] == "(":
            depth += 1
        elif s[i] == ")":
            depth -= 1
        i += 1
    assert depth == 0 and s[i:s.index("\n", i)].strip() == ""
    return s[:start] + s[s.index("\n", i)+1:]


def patch_screen(original):
    s = original
    start_action = '        PrimaryAction(if (loading) "Bulmaca hazırlanıyor…" else "Bulmacayı başlat",'
    # Keep the exact Start callback and all content that follows it in the footer.
    # The original page is the last statement in NewGameScreen, before its helpers.
    page_start = '    ScreenPage("Yeni Oyun", "Kelime havuzunu ve tahta boyutunu seç", back) {'
    if page_start not in s:
        candidates = re.findall(r'(?m)^    ScreenPage\("Yeni Oyun", "[^"\n]*", back\) \{', s)
        assert len(candidates) == 1, candidates
        page_start = candidates[0]
    i = s.index(page_start)
    action = s.index(start_action, i)
    close = s.index("\n    }\n}", action)
    body = s[i+len(page_start):action]
    footer = s[action:close]
    assert "session.newGameFiltered(levels, topics, kinds, count!!)" in footer
    # Do not change persistence, validation, callback or game creation.
    body = one(body,
        '        Wq197CompactHero(levels.first(),session.profileKey,available)\n', '')
    body = remove_call(body, "Wq175NewGameSummary")
    # v197 defines the start validation inside the old page content.
    # After splitting content and sticky footer, move that complete expression
    # into the shared enclosing scope so the original callback and enabled
    # argument can both access it. Do not change the validation conditions.
    valid_match = re.search(r"(?m)^        val valid = !loading && bank != null && count != null[^\n]*(?:\n[ \t]{12,}[^\n]*)*", body)
    assert valid_match is not None, "missing original Start validation"
    valid_decl = valid_match.group(0)
    assert "val valid =" not in footer
    body = body[:valid_match.start()] + body[valid_match.end():]
    footer = valid_decl + "\n" + footer
    assert footer.count("val valid =") == 1
    assert "val valid =" not in body

    body = one(body,
        '''            Wq197OptionGrid(
                listOf("▤ Konu Seç", "◇ Kelime Türü Seç"),
                setOf(if (filterMode == Wq196FilterPolicy.TOPIC)
                    "▤ Konu Seç" else "◇ Kelime Türü Seç"), !loading
            ) { label ->''',
        '''            Wq198FilterTabs(filterMode == Wq196FilterPolicy.TOPIC, !loading) { label ->''')
    body = one(body,
        "                    val limit = Wq197LayoutPolicy.previewLimit(maxWidth.value)",
        '''                    val limit = if (LocalWq198Short.current && maxWidth < 500.dp) 2
                        else if (maxWidth >= 600.dp) 5
                        else 4''')
    body = one(body, "                if (choices.size > 4) {",
                      "                if (choices.size > 2) {")
    # Put duration, word count, and game mode in their own columns on tablets.
    # Phone gets one compact row per short numerical group.
    begin = body.index("                    if (selectedClock == PuzzleClockMode.Countdown) {")
    finish = body.index("            val countChoices: @Composable () -> Unit = {", begin)
    duration_and_closing = body[begin:finish]
    assert duration_and_closing.endswith("                }\n            }\n")
    duration_body = duration_and_closing[:-len("                }\n            }\n")]
    body = body[:begin] + '''                }
            }
            val durationChoices: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
''' + duration_body + '''                }
            }
''' + body[finish:]
    body = one(body,
        '''                if (maxWidth >= 620.dp) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) { modeChoices() }
                        Column(Modifier.weight(1f)) { countChoices() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        modeChoices()
                        countChoices()
                    }
                }''',
        '''                if (maxWidth >= 620.dp) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Column(Modifier.weight(1f)) { modeChoices() }
                        Column(Modifier.weight(1f)) { countChoices() }
                        if (selectedClock != PuzzleClockMode.Classic)
                            Column(Modifier.weight(1f)) { durationChoices() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        modeChoices()
                        countChoices()
                        durationChoices()
                    }
                }''')
    # Avoid duplicate summary/hero above pinned CTA.
    footer = one(footer, '"Bulmacayı başlat"', '"Oyuna Başla"')
    # Any early exit originally labelled with the old composable must retain its target.
    body = body.replace("return@ScreenPage", "return@Wq198NewGamePage")
    footer = footer.replace("return@ScreenPage", "return@Wq198NewGamePage")
    s = (s[:i] + '    Wq198NewGamePage(back, levels.joinToString(" · "), available, footer = {\n'
         + footer + '\n    }) {' + body + s[close:])
    # NewGameSection is a helper private to this file; no other screen uses it.
    start = s.index("\n@Composable\nprivate fun NewGameSection(")
    s = s[:start] + '''
@Composable
private fun NewGameSection(
    title: String, subtitle: String,
    body: @Composable ColumnScope.() -> Unit,
) {
    Wq198Card {
        if (title == "Oyun Ayarları")
            Wq198SectionHeading("settings", title, subtitle)
        else if (title != "Kelime Havuzu")
            Text(title, fontFamily = Serif, color = Ink,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
        body()
    }
}
'''
    for keep in (
        'session.newGameFiltered(levels, topics, kinds, count!!)',
        'MAX_NEW_GAME_WORDS = 70', 'fun toggleLevel(value: String)',
        'PuzzleClockStore(context, session.profileKey).save(',
        'Wq196FilterPolicy.activeTopics(', 'Wq196FilterPolicy.activeKinds(',
        'check(topics.isEmpty() || kinds.isEmpty())',
        'Wq197FilterDialog(', 'if (customTime) OutlinedTextField(',
        'if (customCount) OutlinedTextField(',
    ):
        assert keep in original and keep in s, keep
    return s


def patch_options(original):
    s = original
    s = one(s, 'import androidx.compose.foundation.layout.width',
                'import androidx.compose.foundation.layout.width')
    s = one(s, 'val columns = Wq197LayoutPolicy.columns(maxWidth.value, options.size, longest)',
        '''val columns = when {
            options == englishLevels -> if (maxWidth >= 480.dp) 6 else 3
            options.size == 6 && longest <= 5 && maxWidth >= 300.dp -> 6
            else -> Wq197LayoutPolicy.columns(maxWidth.value, options.size, longest)
        }''')
    s = one(s, 'Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {',
               'Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {')
    s = one(s, 'horizontalArrangement = Arrangement.spacedBy(5.dp)) {',
               'horizontalArrangement = Arrangement.spacedBy(3.dp)) {')
    s = one(s, 'modifier = Modifier.weight(1f).heightIn(min = 40.dp),',
               'modifier = Modifier.weight(1f).heightIn(min = 40.dp),')
    s = one(s, 'containerColor = if (active) LightLime else White,',
               'containerColor = if (active) Color(0xFFE8F4BB) else Color(0xFFFFFEFA),')
    begin = s.index('@Composable\ninternal fun Wq197LevelSelector(')
    end = s.index('/** Remaining topic/kind choices', begin)
    s = s[:begin] + '''@Composable
internal fun Wq197LevelSelector(
    levels: List<String>, enabled: Boolean, chooseLevel: (String) -> Unit,
) {
    val yds = levels.firstOrNull() in ydsLevels
    Wq198Card {
        val heading: @Composable () -> Unit = {
            Wq198SectionHeading("level", "Seviye",
                "Birden fazla CEFR veya YDS seviyesi seçebilirsin")
        }
        val options: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Wq197OptionGrid(listOf("CEFR", "YDS"),
                    setOf(if (yds) "YDS" else "CEFR"), enabled) { label ->
                    if ((label == "YDS") != yds)
                        chooseLevel(if (label == "YDS") ydsLevels.first() else "A1")
                }
                if (!yds) Wq197OptionGrid(englishLevels, levels.toSet(), enabled, chooseLevel)
                else Wq197OptionGrid(ydsLevels.map { "YDS $it" },
                    levels.map { "YDS $it" }.toSet(), enabled) {
                    chooseLevel(it.removePrefix("YDS "))
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 650.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.width(155.dp)) { heading() }
                    Column(Modifier.weight(1f)) { options() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    heading()
                    options()
                }
            }
        }
    }
}

''' + s[end:]
    return s


def main():
    art = read_art()
    screen = patch_screen(SCREEN.read_text("utf-8"))
    ui = patch_options(UI.read_text("utf-8"))
    # No writes until all source anchors and all artwork checks succeed.
    ASSETS.mkdir(parents=True, exist_ok=True)
    for name, data in art.items():
        (ASSETS / (name + ".webp")).write_bytes(data)
    SCREEN.write_text(screen, "utf-8")
    UI.write_text(ui, "utf-8")
    shutil.copyfile(HERE / "Wq198NewGameUi.kt", SRC / "Wq198NewGameUi.kt")
    print("PASS: six approved local art assets and responsive New Game layout installed")
    print("PASS: existing multi-level, filter, clock, count, Start callbacks retained")
    print("PASS: no crossword board or puzzle logic files changed")


if __name__ == "__main__":
    main()
