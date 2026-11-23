#!/usr/bin/env python3
"""v190: Move Lena's redundant mode/tool rows into one top-right overflow menu.

Applies after P5/P7. Retains every original TeacherMode and tool callback,
voice settings, composer, message rendering and user thread persistence.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"

def once(s,old,new,label):
    n=s.count(old)
    if n!=1: raise AssertionError(f"{label}: expected 1 match, found {n}")
    return s.replace(old,new,1)

def apply():
    planned={}
    f=SRC/"LenaMessengerBrand.kt"
    s=f.read_text("utf-8")
    start=s.index("internal fun LenaMessengerModeBar(")
    end=s.index("\n}\n\n/** Decorative speaking indicator",start)+2
    original=s[start:end]
    assert 'LenaModePill("Mesajlar"' in original
    assert 'LenaModePill("Pratik"' in original
    assert 'Text("Diğer ⋯"' in original
    assert 'onVocabulary()' in original

    replacement="""internal fun LenaMessengerModeBar(
    selected: TeacherMode,
    onSelect: (TeacherMode) -> Unit,
    onVocabulary: () -> Unit,
    onLessons: () -> Unit = {},
) {
    var extraOpen by remember { mutableStateOf(false) }
    var showModeHighlight by remember { mutableStateOf(false) }
    // Only one compact 3-dot control occupies the top-right chat corner.
    // The prior full-width Mesajlar/Pratik/Diğer bar is removed.
    Box(Modifier.fillMaxWidth().height(36.dp),contentAlignment=Alignment.TopEnd) {
        Box {
            androidx.compose.material3.IconButton(
                onClick={extraOpen=true},modifier=Modifier.size(34.dp)
            ) {
                Text("⋮",fontSize=25.sp,color=LenaInk,
                    modifier=Modifier.semantics {
                        contentDescription="Sohbet ve ders araçları"
                    })
            }
            DropdownMenu(expanded=extraOpen,onDismissRequest={extraOpen=false}) {
                DropdownMenuItem(text={Text("Mesajlar")},onClick={
                    extraOpen=false;showModeHighlight=false
                })
                DropdownMenuItem(text={Text("Pratik")},onClick={
                    extraOpen=false;showModeHighlight=true
                    onSelect(TeacherMode.Practice)
                })
                DropdownMenuItem(text={Text("Dilbilgisi")},onClick={
                    extraOpen=false;showModeHighlight=true
                    onSelect(TeacherMode.Grammar)
                })
                DropdownMenuItem(text={Text("Kişisel ders")},onClick={
                    extraOpen=false;showModeHighlight=true
                    onSelect(TeacherMode.Lesson)
                })
                DropdownMenuItem(text={Text("Hata düzelt")},onClick={
                    extraOpen=false;showModeHighlight=true
                    onSelect(TeacherMode.Correction)
                })
                DropdownMenuItem(text={Text("Kelime araçları")},onClick={
                    extraOpen=false;onVocabulary()
                })
                DropdownMenuItem(text={Text("Ders araçları")},onClick={
                    extraOpen=false;onLessons()
                })
            }
        }
    }
}"""
    s=s[:start]+replacement+s[end:]
    s=once(s,"import androidx.compose.ui.draw.shadow",
        "import androidx.compose.ui.draw.shadow\nimport androidx.compose.ui.semantics.semantics\nimport androidx.compose.ui.semantics.contentDescription",
        "accessibility for three-dot overflow button")
    planned[f]=s

    f=SRC/"ConversationScreen.kt"
    s=f.read_text("utf-8")
    marker="LenaMessengerModeBar("
    assert s.count(marker)==1, "Expected a single active Lena mode bar call"
    at=s.index(marker)+len(marker)
    n=1
    inside=False
    escaped=False
    closing=None
    for i in range(at,len(s)):
        c=s[i]
        if inside:
            if escaped: escaped=False
            elif c=="\\": escaped=True
            elif c=='"': inside=False
            continue
        if c=='"':
            inside=True
        elif c=="(": n+=1
        elif c==")":
            n-=1
            if n==0:
                closing=i
                break
    assert closing is not None
    original_call=s[at:closing]
    assert "onVocabulary" in original_call, "Tool callback missing"
    assert "onSelect" in original_call, "Mode callback missing"
    separator="" if original_call.rstrip().endswith(",") else ","
    s=s[:closing]+separator+"\n                onLessons = { learningToolsOpen = true }\n            "+s[closing:]

    # Remove the extra tool bar; the exact same dialog is accessible from ⋮.
    old="""            if (!keyboardVisible) Row(
                Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,
            ) {
                TextButton(onClick = { learningToolsOpen = true }) {
                    Text("Ders araçları  ⚙", color=Color(0xFF276446),
                        fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                }
            }
"""
    s=once(s,old,"","remove prominent extra lesson tools row")
    old="""            if (!keyboardVisible) Row(Modifier.fillMaxWidth().padding(vertical=3.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD4E5D9)))
                Text("Sohbet",color=Color(0xFF2C7950),fontSize=12.sp,
                    fontWeight=FontWeight.SemiBold)
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD4E5D9)))
            }
"""
    s=once(s,old,"","remove redundant decorative separator to expand chat history")
    assert "if (learningToolsOpen)" in s
    assert "LazyColumn(" in s
    assert "LenaRichComposer(" in s
    assert "learningToolsOpen = true" in s
    planned[f]=s

    for p,text in planned.items(): p.write_text(text,"utf-8")
    print("PASS v190 Lena: one top-right three-dot control")
    print("PASS v190 Lena: lessons, modes and vocabulary remain accessible")
    print("PASS v190 Lena: two fixed-height rows removed, chat area expanded")

if __name__=="__main__": apply()
