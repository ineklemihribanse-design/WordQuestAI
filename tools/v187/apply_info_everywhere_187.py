#!/usr/bin/env python3
"""Paket 7: all-app informational-copy audit + opt-in inline info controls.

Runs ONLY on the fully assembled Android tree after packages 4–6. It never
modifies stored questions, imported content, user data, Firebase, manifests,
or previously staged build scripts.
"""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
CATEGORIES = {
    "Conversation": "Sohbet ve öğrenme",
    "LiveTeacher": "Lena ile konuşma",
    "Arena": "Arena hakkında",
    "PremiumOnline": "Maç ve kurallar",
    "Wq174": "Maç ve kurallar",
    "Wq175": "Bu bölüm hakkında",
    "Wq186": "Kullanım bilgisi",
    "WordQuestSettings": "Ayarlar hakkında",
    "AiProviderSettings": "Veri ve yapay zekâ",
    "Update": "Güncelleme bilgisi",
    "Puzzle": "Bulmaca nasıl çalışır?",
    "Home": "Uygulama hakkında",
    "Practice": "Alıştırma hakkında",
    "Learning": "Öğrenme hakkında",
    "Vocabulary": "Kelime çalışması",
    "Yds": "Sınav ve çalışma",
}
# Explain how an existing feature works; don't hide a question, question
# instruction, substantive lesson passage, current error or action label.
EXPLAIN = re.compile(
    r"(?iu)(uygulama|gizlilik|anahtar|konuşma|sohbet|sesler|ses motor|telaffuz|"
    r"ayarlar|çevrimdışı|cihaz|otomatik|önceki|kayıt|kaydedil|sistem|"
    r"maç|bulmaca|ipucu|puanlam|süre|kullan|dil|veri|yükle|"
    r"gönder|profil|içerik|seçili|seçenek|erişim|güvenli|"
    r"güncelle|öğrenme|yapabilir|yapılır|burada|etkinleştir|"
    r"yapmanız|göster|liste|senkron|yeniden|başlat|"
    r"ders|hedef|çalışma|kontrol|doğrula|öğretmen|"
    r"hesap|kelime|özellik|mod|mikrofon|oyuncu|"
    r"belge|dosya|satır|seviy|kategor|cevap|"
    r"import|oluştur|dinle|yanıt|soru|oda|"
    r"model|sağlayıcı|banka|bakım|uyarı|bilgi)"
)
LITERAL_CHAIN = re.compile(r'\s*"(?:\\.|[^"\\])*"(?:\s*\+\s*"(?:\\.|[^"\\])*")*\s*\Z',re.S)
LITERAL = re.compile(r'"(?:\\.|[^"\\])*"',re.S)
CALL = re.compile(r'(?<![A-Za-z0-9_])(?:Text|BodyText)\s*\(')
SKIP_NAMES = {
    "PracticeContent.kt","YdsContent.kt","YdsExtraContent.kt","Catalog.kt",
    "Conversation.kt","ConversationAudio.kt","PremiumArenaContent.kt",
    "Wq187InfoTip.kt","Wq187InfoAndIcons.kt",
}
SKIP_FILE_PREFIXES = ("Wq187",)
SKIP_CALL_ARGUMENTS = ("Modifier.weight(","Modifier.align(","Modifier.offset(",
                       "Modifier.matchParentSize(")

def scan_call(src,start):
    """Find composable call end and its first top-level argument safely."""
    pos=start
    depth=1
    first_comma=None
    in_string=False
    triple=False
    escaped=False
    while pos<len(src):
        c=src[pos]
        if in_string:
            if triple:
                if src.startswith('"""',pos):
                    in_string=False;triple=False;pos+=3;continue
            elif escaped:
                escaped=False
            elif c=="\\":
                escaped=True
            elif c=='"':
                in_string=False
        elif src.startswith('"""',pos):
            in_string=True;triple=True;pos+=3;continue
        elif c=='"':
            in_string=True
        elif c=='(':
            depth+=1
        elif c==')':
            depth-=1
            if depth==0:
                return pos, first_comma if first_comma is not None else pos
        elif c==',' and depth==1 and first_comma is None:
            first_comma=pos
        pos+=1
    return None,None

def title(name,value):
    lower=value.lower()
    if any(t in lower for t in ("anahtar","veri güven","gizlilik","şifreli","firebase","şahsi")):
        return "Gizlilik ve veri kullanımı"
    if any(t in lower for t in ("ses motoru","aksanın","telaffuz","espeak","fonetik")):
        return "Ses ve telaffuz bilgisi"
    if any(t in lower for t in ("maç","rakip","ipucu","puanlama","duello","bulmaca")):
        return "Oyun nasıl çalışır?"
    if "güncelle" in lower or "apk" in lower:
        return "Güncelleme ve güvenlik"
    return next((v for k,v in CATEGORIES.items() if name.startswith(k)),
                "Bu özellik hakkında")

def applicable(name,expr,remaining):
    if name in SKIP_NAMES or name.startswith(SKIP_FILE_PREFIXES):return False
    if not LITERAL_CHAIN.fullmatch(expr):return False
    if any(block in remaining for block in SKIP_CALL_ARGUMENTS):return False
    literals=LITERAL.findall(expr)
    if not literals:return False
    value="".join(x[1:-1] for x in literals)
    if len(value)<92:return False
    if not EXPLAIN.search(value):return False
    # Avoid replacing dramatic exercise content or static prompts; info text is
    # typically explanatory prose, not a list of answer options or vocabulary.
    if ("a)" in value and "b)" in value and "c)" in value):return False
    return True

def transform_file(path):
    src=path.read_text("utf-8")
    if "@Composable" not in src or not CALL.search(src):return (0,src,[])
    edits=[]
    for match in CALL.finditer(src):
        # Don't reinterpret quoted sample source or comments as real UI calls.
        line_start=src.rfind("\n",0,match.start())+1
        before=src[line_start:match.start()]
        if '//' in before or '"' in before or "/*" in before:continue
        end,comma=scan_call(src,match.end())
        if end is None:continue
        expr=src[match.end():comma].strip()
        rest=src[comma:end] if comma != end else ""
        if not applicable(path.name,expr,rest):continue
        value="".join(x[1:-1] for x in LITERAL.findall(expr))
        label=title(path.name,value)
        new=f'Wq187InfoTip("{label}", {expr})'
        edits.append((match.start(),end+1,new,label,value[:85]))
    if not edits:return (0,src,[])
    # Ensure disjoint syntax range. Nested Text calls aren't eligible as
    # argument-only literal chains, but verify to prevent double replacement.
    previous=len(src)+1
    for a,b,new,_,_ in reversed(edits):
        if b>previous:raise AssertionError(f"Overlapping Text call in {path.name}")
        src=src[:a]+new+src[b:]
        previous=a
    return len(edits),src,[{"title":label,"example":value} for _,_,_,label,value in edits]

def main():
    assert (APP/"Wq186ArenaFairPlay.kt").exists()
    assert not (APP/"Wq187InfoTip.kt").exists()
    planned=[]
    for path in sorted(APP.rglob("*.kt")):
        count,content,explanations=transform_file(path)
        if count:planned.append((path,content,explanations))
    # Long text is audited across the whole compiled app, not only settings.
    if len(planned)<4 or sum(len(e) for _,_,e in planned)<8:
        raise AssertionError("Too few genuine explanatory UI paragraphs; audit required")
    for path,content,_ in planned:
        path.write_text(content,"utf-8")
    import shutil
    source=ROOT/"tools/v187/Wq187InfoTip.kt"
    assert source.is_file()
    shutil.copyfile(source,APP/"Wq187InfoTip.kt")
    audit={"pages":len(planned),
           "info_controls":sum(len(e) for _,_,e in planned),
           "placements":{str(p.relative_to(APP)):examples for p,_,examples in planned}}
    out=ROOT/"source/package7_info_audit.json"
    out.write_text(json.dumps(audit,ensure_ascii=False,indent=2)+"\n","utf-8")
    print("PASS Paket 7 INFO: pages",audit["pages"],
          "info controls",audit["info_controls"])
    for path,_,examples in planned:
        print(" INFO_PAGE",path.name,len(examples))
    print("No exercise content, user data, Firebase or app identity changed.")

if __name__=="__main__":
    main()
