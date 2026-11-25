#!/usr/bin/env python3
"""Exercise the actual signed release candidate through normal Android UI.
No account, Firebase credentials, app-data clearing, or debug modification."""
import os, time, re, subprocess, xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
OUT=Path(os.environ["WQ_RELEASE_DIR"])
PKG="com.kelimebulmacasi.a1.practice.fresh"
# Reuse the previously verified home-to-Arena route, without running its old tests.
source=Path("tools/v121_arena/qa_fullscreen.py").read_text()
prefix=source[:source.index('\ntry:\n    viewport(')]
exec(compile(prefix,"qa_fullscreen_helpers","exec"),globals())
def launch_foreground():
    # Only recover a known emulator system launcher ANR; never dismiss
    # a WordQuest crash, login, or permission dialog as a passing test.
    adb("shell","am","force-stop",PKG)
    for attempt in range(5):
        adb("shell","am","start","-W","-n",PKG+"/com.kelimebulmacasi.a1.MainActivity")
        time.sleep(5)
        ns=nodes()
        labels=" ".join(label(n) for n in ns)
        if "pixel launcher isn't responding" in labels or "pixel launcher keeps stopping" in labels:
            print("Recovering emulator Pixel Launcher ANR",flush=True)
            close=next((n for n in ns if label(n).strip()=="close app"),None)
            if close is not None:tap(close)
            adb("shell","am","force-stop","com.google.android.apps.nexuslauncher")
            time.sleep(2)
            continue
        if any(n.attrib.get("package")==PKG for n in ns):
            print("PASS: WordQuest foreground after launch",flush=True)
            return
        print("Foreground not ready:",labels[:700],flush=True)
    raise AssertionError("WordQuest foreground could not be verified")
def capture(name):
    ns=nodes()
    (OUT/f"arena-{name}.xml").write_text(adb("exec-out","cat","/sdcard/wq121.xml"))
    (OUT/f"arena-{name}.png").write_bytes(subprocess.check_output(["adb","exec-out","screencap","-p"],timeout=35))
    return ns
def visible(ns,pattern):
    candidates=[n for n in ns if re.search(pattern,label(n).strip(),re.I)]
    return next((n for n in candidates if n.attrib.get("clickable")=="true"),candidates[-1] if candidates else None)
def scroll(direction):
    size=adb("shell","wm","size")
    w,h=map(int,re.findall(r"(\d+)x(\d+)",size)[-1])
    adb("shell","input","swipe",str(w//2),str(int(h*(.80 if direction=="down" else .28))),str(w//2),str(int(h*(.28 if direction=="down" else .80))),"400")
    time.sleep(.6)
def find(pattern,allow_scroll=True):
    for i in range(12):
        ns=nodes();n=visible(ns,pattern)
        print("Find",pattern,i,[label(x).strip() for x in ns if label(x).strip()][-35:],flush=True)
        if n is not None:return n
        if allow_scroll:scroll("up" if i<3 else "down")
        else:time.sleep(1)
    capture("missing-control")
    raise AssertionError("Missing control: "+pattern)
def selected_chip(value):
    ns=nodes()
    root=ET.fromstring(adb("exec-out","cat","/sdcard/wq121.xml"))
    parents={child:parent for parent in root.iter("node") for child in parent}
    for n in root.iter("node"):
        if label(n).strip()!=value.casefold():continue
        p=parents.get(n)
        if p is not None and any(label(x).strip()=="✓" for x in p.iter("node")):return True
        if n.attrib.get("selected")=="true" or n.attrib.get("checked")=="true":return True
    return False
START=r"oyuna başla|oyunu başlat|yarışmayı başlat|maçı başlat|^başlat$|^oyna$"
def select_pool():
    for level in ("A1","A2","B1","B2","C1","C2"):
        n=find(r"^"+level+r"$")
        if not selected_chip(level):
            tap(n);time.sleep(.5)
        assert selected_chip(level),"Level did not select: "+level
def start_mode(mode):
    find_arena(mode)
    tap(find(r"^"+mode));find(r"maç ayarları")
    tap(find(r"tek başına oyna"));time.sleep(1.5)
    select_pool()
    kind="quiz" if "Şıklı" in mode else "crossword"
    capture("settings-"+kind)
    tap(find(START));time.sleep(2)
    # The first start hands off from online Arena to the local Arena
    # mode picker. This is a normal navigation step, not a round start.
    ns=nodes()
    if visible(ns,r"oyun modları") is not None:
        print("Local Arena handoff detected; choosing requested mode",flush=True)
        capture("local-picker-"+kind)
        tap(find(r"^"+mode));find(r"maç ayarları")
        select_pool()
        capture("local-settings-"+kind)
        tap(find(START));time.sleep(2)
    # Never treat a lobby as a successful gameplay screen.
def folded(value):
    import unicodedata
    return "".join(c for c in unicodedata.normalize("NFKD",value.casefold().replace("ı","i")) if not unicodedata.combining(c))
def wait_game(mode,wide=False):
    deadline=time.monotonic()+65
    while time.monotonic()<deadline:
        ns=nodes()
        labels=[label(n).strip() for n in ns if label(n).strip()]
        all_labels=folded(" | ".join(labels))
        print("Game UI:",all_labels[:2000],flush=True)
        if "oyun hazirlanamadi" in all_labels or "web motoru yanit vermedi" in all_labels:
            capture("engine-error");raise AssertionError("Android game engine failed: "+all_labels[:1000])
        # Match the ACTUAL observed v132 gameplay UI: the answer controls
        # are below the viewport on the 30-word board. Never require them
        # just to prove the game is running.
        if mode=="crossword" and all(t in all_labels for t in ("kelime bulmacasi","tum tahta","ilerleme")):
            count=re.search(r"ilerleme\s*\d+/(20|30|40|50)\b",all_labels)
            cells=sum(x.isdigit() for x in labels)
            assert count and cells>=4,(all_labels,cells)
            assert "a1 + a2 + b1 + b2 + c1 + c2" in all_labels,all_labels
            if wide:
                # No scrolling before this check: the answer panel must be
                # visible beside the board, not below the viewport.
                assert all(t in all_labels for t in ("kelimeyi yaz","cevabi gonder","pas gec","harf ipucu")),all_labels
                cellnodes=[n for n in ns if label(n).strip().isdigit()]
                field=next(n for n in ns if "kelimeyi yaz" in folded(label(n)))
                assert max(bounds(n)[2] for n in cellnodes)<bounds(field)[0],(bounds(field),[bounds(n) for n in cellnodes])
                capture("tablet-crossword-started")
                img=Image.open(OUT/"arena-tablet-crossword-started.png")
                assert img.width>img.height
            else:capture("web-crossword-started")
            print("Verified real crossword progress and numbered cells:",count.group(1),cells,flush=True)
            return
        if mode=="quiz" and "ingilizcesi hangisidir" in all_labels:
            assert "mac ayarlari" not in all_labels
            assert "a1 + a2 + b1 + b2 + c1 + c2" in all_labels,all_labels
            capture("tablet-quiz-started" if wide else "web-quiz-started")
            return
        time.sleep(2)
    capture("engine-timeout");raise AssertionError("Game did not reach actual gameplay: "+mode)
try:
    adb("shell","wm","size","1080x2340");adb("shell","wm","density","360")
    launch_foreground()
    start_mode("Bulmaca Düellosu");wait_game("crossword")
    print("PASS: actual Android mixed crossword gameplay opens",flush=True)
    launch_foreground()
    start_mode("5 Şıklı Düello")
    wait_game("quiz")
    print("PASS: actual Android web quiz gameplay opens",flush=True)
    adb("shell","wm","size","1680x978");adb("shell","wm","density","240")
    adb("shell","settings","put","system","accelerometer_rotation","0")
    adb("shell","settings","put","system","user_rotation","0")
    launch_foreground()
    start_mode("Bulmaca Düellosu");wait_game("crossword",wide=True)
    print("PASS: landscape Arena board, answers and mixed-level title",flush=True)
    launch_foreground()
    start_mode("5 Şıklı Düello");wait_game("quiz",wide=True)
    adb("shell","input","keyevent","KEYCODE_BACK");time.sleep(2)
    ns=capture("tablet-quiz-return")
    labels=folded(" | ".join(label(n) for n in ns))
    assert "oyun modlari" in labels and "soru sayisi" not in labels,labels
    print("PASS: landscape Arena quiz and clean return",flush=True)
finally:
    (OUT/"android-logcat.txt").write_text(adb("logcat","-d","-t","2000"))
    adb("shell","wm","size","reset");adb("shell","wm","density","reset")
