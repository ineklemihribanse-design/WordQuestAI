#!/usr/bin/env python3
"""v1.0.88: repair focus-recomposition crash, use approved home icons, and
raise mutual-friend preview fidelity without changing the Firebase schema.
All updates fail closed on changed upstream sources. No account data reset.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v188"

def source(name):
    p=SRC/name
    assert p.is_file(),("Missing fully assembled source",name)
    return p,p.read_text("utf-8")

def once(s,old,new,label):
    n=s.count(old)
    assert n==1,(label,n)
    return s.replace(old,new,1)

def apply():
    planned={}
    p,s=source("PuzzleScreen.kt")
    # The actual crash: the IME changed the tablet's 'wide' branch, disposing
    # the focused TextField while its FocusRequester was trying to focus it.
    s=once(s,"val wide = maxWidth >= 760.dp && !keyboardVisible",
           "val wide = maxWidth >= 760.dp",
           "preserve tablet editor parent when keyboard opens")
    s=once(s,'''            entryFocus.requestFocus()
            softKeyboard?.show()''',
           '''            // Wait for the visible editor to attach before requesting focus.
            // Never crash the game if focus is temporarily unavailable.
            withFrameNanos { }
            runCatching {
                entryFocus.requestFocus()
                softKeyboard?.show()
            }''',"safe IME focus")
    s=once(s,'''                    if (keyboardVisible) {
                        ActiveClue(word, view.wordIndex in view.solvedIndices, true) {
                            if (view.wordIndex !in view.solvedIndices) inlineTyping = true
                        }
                        inlineEditor()
                    }
                    BoardFrame(''',
           '''                    // Phone editor is composed once, in the same place with IME open.
                    ActiveClue(word, view.wordIndex in view.solvedIndices, keyboardVisible) {
                        if (view.wordIndex !in view.solvedIndices) inlineTyping = true
                    }
                    inlineEditor()
                    BoardFrame(''',"do not reparent phone text field")
    s=once(s,'''                    if (!keyboardVisible) {
                        ActiveClue(word, view.wordIndex in view.solvedIndices, false) {
                            if (view.wordIndex in view.solvedIndices) listen?.invoke()
                            else inlineTyping = true
                        }
                    }
                    if (!keyboardVisible) inlineEditor()
                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)''',
           '''                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)''',
           "remove duplicate editor under board")
    assert s.count("inlineEditor()")==2
    assert 'title={Text("Kelimeyi yaz")}' not in s
    planned[p]=s

    p,s=source("WordQuest175Icons.kt")
    s=once(s,'            "öğrenme merkez" in s || "öneri" in s -> "insights"',
           '            "ana sayfa" in s || s == "ana" -> "home"\n'
           '            "tek kişilik" in s || "tek başına" in s -> "solo_puzzle"\n'
           '            "yeni oyun" in s -> "new_game"\n'
           '            "öğrenme merkez" in s || "öneri" in s -> "insights"',
           "home/solo/new game semantic icon routes")
    planned[p]=s

    p,s=source("Wq182Icons.kt")
    s=once(s,'        "arena_duel" to "wq_bulmaca_duellosu",',
           '''        "home" to "wq_ana",
        "new_game" to "wq_yeni_oyun",
        "solo_puzzle" to "wq_bulmacalarim",
        "folder" to "wq_hazir_paketler",
        "arena_duel" to "wq_bulmaca_duellosu",''',
           "use distinct approved game/folder/home pictures")
    planned[p]=s

    p,s=source("Wq175PremiumHome.kt")
    s=once(s,'''                if(tile.artSlug=="bulmacalarim" &&
                    Wq182Artwork("wq_bulmacalarim",Modifier.size(imageSize))) {
                    // A unique puzzle illustration, never the Arena team symbol.
                } else Wq175GlossIcon(''',
           '''                val illustrated=when(tile.title) {
                    "Hazır Paketler" -> "wq_hazir_paketler"
                    "Bulmacalarım" -> "wq_bulmacalarim"
                    else -> null
                }
                if(illustrated!=null &&
                    (Wq183HighResArtwork(illustrated,Modifier.size(imageSize)) ||
                     Wq182Artwork(illustrated,Modifier.size(imageSize)))) {
                    // Dedicated approved artwork: suppress repeated generic symbols.
                } else Wq175GlossIcon(''',
           "actual home folder and solo crossword icon")
    s=once(s,'            Wq175GlossIcon("puzzle",Modifier.size(59.dp))',
           '''            if(!Wq183HighResArtwork("wq_yeni_oyun",Modifier.size(59.dp)) &&
               !Wq182Artwork("wq_yeni_oyun",Modifier.size(59.dp)))
                Wq175GlossIcon("new_game",Modifier.size(59.dp))''',
           "new game must have its own illustrated icon")
    planned[p]=s

    # Friends-only preview: the live database allows 18,050 characters for the
    # optional mutually-readable thumbnail. Preserve that validated limit.
    # Arena still sends its strict 160px images through Wq183ArenaShare.
    p,s=source("Wq185FriendMascot.kt")
    s=once(s,'return Wq183ArenaShare.decodePreview(id, preview)',
           'return Wq188FriendPreview.decode(id, preview)',
           "friend decoder accepts bounded higher resolution")
    s=once(s,'Wq183ArenaShare.encodePreview(avatar, source)',
           'Wq188FriendPreview.encode(avatar, source)',
           "friend publisher creates higher quality preview")
    planned[p]=s

    p,s=source("Wq186FriendProfileDetail.kt")
    s=once(s,'import androidx.compose.ui.graphics.Color',
           'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.FilterQuality',
           "friend profile bilinear/trilinear filtering")
    s=once(s,'''                                    modifier=Modifier.fillMaxSize(),
                                    contentScale=ContentScale.Fit)''',
           '''                                    modifier=Modifier.fillMaxSize(),
                                    contentScale=ContentScale.Fit,
                                    filterQuality=FilterQuality.High)''',
           "friend portrait smooth scaling")
    planned[p]=s

    p,s=source("Wq180FriendCards.kt")
    s=once(s,
           'contentScale=androidx.compose.ui.layout.ContentScale.Fit)',
           'contentScale=androidx.compose.ui.layout.ContentScale.Fit,\n'
           '                filterQuality=androidx.compose.ui.graphics.FilterQuality.High)',
           "friend tile smooth scaling")
    planned[p]=s

    codec=(PKG/"Wq188FriendPreview.kt").read_text("utf-8")
    assert "MAX_SIDE = 1024" in codec and "MAX_RAW = 13000" in codec
    assert 'for(edge in listOf(1024, 896, 768, 640, 512, 448, 384, 320, 256, 192, 160))' in codec
    planned[SRC/"Wq188FriendPreview.kt"]=codec
    for path,data in planned.items():
        path.write_text(data,encoding="utf-8")
        print("PASS v188",path.name)
    print("PASS: stable inline IME, four approved icon routes, quality-bounded friend preview")

if __name__=="__main__":
    apply()
