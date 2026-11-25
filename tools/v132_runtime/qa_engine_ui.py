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
        if n is not None:return n
        if allow_scroll:scroll("up" if i<3 else "down")
        else:time.sleep(1)
    capture("missing-control")
    raise AssertionError("Missing control: "+pattern)
def start_mode(mode):
    find_arena(mode)
    tap(find(r"^"+mode))
    find(r"maç ayarları")
    solo=find(r"tek başına oyna")
    tap(solo)
    time.sleep(1.5)
    # Some routes switch to the dedicated local mode picker first.
    ns=nodes()
    if visible(ns,r"oyun modları") is not None and visible(ns,r"maç ayarları") is None:
        tap(find(r"^"+mode));find(r"maç ayarları")
    for level in ("A2","B1"):
        n=find(r"^"+level+r"$")
        # Fresh install starts at A1; use checked/selected if exposed.
        if n.attrib.get("selected")!="true" and n.attrib.get("checked")!="true":tap(n)
    capture("settings-"+("quiz" if "Şıklı" in mode else "crossword"))
    tap(find(r"oyunu başlat|yarışmayı başlat|maçı başlat|^başlat$|^oyna$"))
def wait_game(mode):
    deadline=time.monotonic()+65
    while time.monotonic()<deadline:
        ns=nodes()
        all_labels=" | ".join(label(n) for n in ns if label(n).strip())
        print("Game UI:",all_labels[:1800],flush=True)
        if "oyun hazırlanamadı" in all_labels or "web motoru yanıt vermedi" in all_labels:
            capture("engine-error");raise AssertionError("Android game engine failed: "+all_labels[:1000])
        # Native game displays a crossword answer/hint control or a real five-choice prompt.
        if mode=="crossword" and ("harf aç" in all_labels or "bilmiyorum" in all_labels or "cevabı yaz" in all_labels) and "maç ayarları" not in all_labels:
            capture("web-crossword-started");return
        if mode=="quiz" and ("ingilizcesi hangisidir" in all_labels or "ingilizcesi hangisi" in all_labels):
            capture("web-quiz-started");return
        time.sleep(2)
    capture("engine-timeout");raise AssertionError("Game did not reach actual gameplay: "+mode)
try:
    adb("shell","wm","size","1080x2340");adb("shell","wm","density","360")
    adb("shell","am","force-stop",PKG)
    adb("shell","am","start","-n",PKG+"/com.kelimebulmacasi.a1.MainActivity")
    time.sleep(8)
    start_mode("Bulmaca Düellosu");wait_game("crossword")
    print("PASS: actual Android mixed crossword gameplay opens",flush=True)
    adb("shell","am","force-stop",PKG)
    adb("shell","am","start","-n",PKG+"/com.kelimebulmacasi.a1.MainActivity")
    time.sleep(6)
    # Preset persists from prior game; do not toggle the selected levels twice.
    find_arena("quiz")
    tap(find(r"^5 şıklı düello"));find(r"maç ayarları")
    tap(find(r"tek başına oyna"));time.sleep(1.5)
    ns=nodes()
    if visible(ns,r"oyun modları") is not None and visible(ns,r"maç ayarları") is None:
        tap(find(r"^5 şıklı düello"));find(r"maç ayarları")
    tap(find(r"oyunu başlat|yarışmayı başlat|maçı başlat|^başlat$|^oyna$"))
    wait_game("quiz")
    print("PASS: actual Android web quiz gameplay opens",flush=True)
finally:
    adb("shell","wm","size","reset");adb("shell","wm","density","reset")
