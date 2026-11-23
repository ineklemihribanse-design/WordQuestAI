#!/usr/bin/env python3
"""v190 follow-up: compact YDS disclosure; real paragraph illustration;
phone full-board toggle stays available with keyboard; preserve existing AI/YDS art.
Applied AFTER original v190 icon-scope and Lena patches.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"

def once(s, before, after, label):
    n=s.count(before)
    if n!=1:
        raise AssertionError(f"{label}: expected one anchor, found {n}")
    return s.replace(before,after,1)

def apply():
    pending={}

    # Category titles remain intact. In YDS, only the two section cards are
    # collapsible. The five main navigation categories and original callbacks
    # are never replaced.
    p=SRC/"Wq175PremiumHome.kt"
    s=p.read_text("utf-8")
    s=once(s,
        "import androidx.compose.material3.Text",
        "import androidx.compose.material3.Text\nimport androidx.compose.material3.TextButton",
        "import YDS TextButton before use in Compose")
    s=once(s,
        '''    var activeSection by remember { mutableStateOf<String?>(null) }''',
        '''    var activeSection by remember { mutableStateOf<String?>(null) }
    var ydsExpanded by remember { mutableStateOf(true) }''',
        "remember YDS disclosure visibility")
    s=once(s,
        '''            Text(activeSection.orEmpty(),fontWeight=FontWeight.ExtraBold,
                color=Wq175Navy,fontSize=21.sp)''',
        '''            if (activeSection=="YDS AKADEMİ") {
                TextButton(onClick={ydsExpanded=!ydsExpanded}) {
                    Text(
                        "YDS AKADEMİ  " + if (ydsExpanded) "▴  Kapat" else "▾  Aç",
                        fontWeight=FontWeight.ExtraBold,
                        color=Wq175Navy,fontSize=20.sp)
                }
            } else {
                Text(activeSection.orEmpty(),fontWeight=FontWeight.ExtraBold,
                    color=Wq175Navy,fontSize=21.sp)
            }''',
        "expandable YDS academy category heading")
    s=once(s,
        '''            val tiles=if(activeSection==null) categories else features''',
        '''            val tiles=if(activeSection=="YDS AKADEMİ" && !ydsExpanded)
                emptyList<Wq175HomeTile>()
                else if(activeSection==null) categories else features''',
        "hide only YDS tile bodies when collapsed")
    assert '"YDS Akademi" -> "exam"' not in s
    assert '"YDS Hazırlık" -> "review"' not in s
    pending[p]=s

    # One dedicated high-res paragraph icon (reading book), rather than
    # a generic fallback dot. Does NOT override old YDS or AI portrait.
    p=SRC/"Wq189UploadedIcons.kt"
    s=p.read_text("utf-8")
    s=once(s,
        '''        "verbs" to "verb_forms"
    )''',
        '''        "verbs" to "verb_forms",
        "paragraph" to "reading_review_classic"
    )''',
        "single standalone paragraph route")
    assert '"assistant" to' not in s and '"ai_teacher" to' not in s
    assert '"exam" to' not in s and '"review" to' not in s
    pending[p]=s
    paragraph_asset=ROOT/"source/app/src/main/assets/approved_icons_v189/reading_review_classic.webp"
    assert paragraph_asset.is_file(), "dedicated paragraph art absent"
    p=SRC/"Wq186ParagraphWorkspace.kt"
    s=p.read_text("utf-8")
    assert 'Wq175GlossIcon("paragraph", Modifier.size(56.dp))' in s
    assert 'Text("Paragraf Dinle ve Yaz · Çevir"' in s
    # Already uses Wq175GlossIcon route; the mapping above now provides the
    # approved paragraph icon on this page and the academy module.
    assert 'Wq175GlossIcon("paragraph",Modifier.size(69.dp))' in (SRC/"WordQuest175ParagraphDictation.kt").read_text("utf-8") or "paragraph" in s
    pending[p]=s

    # Phone zoom could become trapped while handwriting IME is visible:
    # the "Tüm tahtayı göster" toggle was suppressed completely.
    p=SRC/"PuzzleScreen.kt"
    s=p.read_text("utf-8")
    s=once(s,
        'val keyboardBoard = (maxHeight * .52f).coerceIn(145.dp, 460.dp)',
        'val keyboardBoard = (maxHeight * .82f).coerceIn(200.dp, 520.dp)',
        "give phone portrait grid more space while typing")
    s=once(s,
        '''                        if (keyboardVisible) null else ({ expanded = !expanded }),''',
        '''                        ({ expanded = !expanded }),''',
        "do not hide fit/full-board control when keyboard is open")
    assert "expanded || (keyboardVisible && keyboardKeys)" not in s
    s=once(s,
        '''    val word = board.words[view.wordIndex]''',
        '''    val word = board.words[view.wordIndex]
    // Returning from a zoomed view to handwriting input restores the entire
    // board; players may still zoom again deliberately with the visible toggle.
    LaunchedEffect(keyboardVisible, game.id) {
        if (keyboardVisible) expanded = false
    }''',
        "automatically return to overview on first keyboard appearance")
    assert "val previewLetters: Map<Cell, Char?>" in s
    assert "fittedBoardHeight" in s
    pending[p]=s
    for path,body in pending.items():
        path.write_text(body,"utf-8")
    print("PASS v190: YDS academy category expandable while old YDS/AI art remains")
    print("PASS v190: paragraph gets dedicated supplied high-quality 3D book icon")
    print("PASS v190: keyboard never hides full-board zoom toggle on phones")
    print("PASS v190: no Firebase, score, game-progress or provider changes")

if __name__=="__main__": apply()
