#!/usr/bin/env python3
"""Real Android UI verification on both portrait and landscape display sizes."""
import os,re,time,subprocess,xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image

PKG="com.kelimebulmacasi.a1.practice.fresh"
OUT=Path(os.environ["WQ_RELEASE_DIR"])
def adb(*args,timeout=40):
    return subprocess.check_output(["adb",*args],stderr=subprocess.STDOUT,timeout=timeout).decode("utf-8","replace")
def nodes():
    adb("shell","uiautomator","dump","/sdcard/wq121.xml",timeout=45)
    return list(ET.fromstring(adb("exec-out","cat","/sdcard/wq121.xml")).iter("node"))
def label(n):
    return " ".join([n.attrib.get("text",""),n.attrib.get("content-desc","")]).casefold()
def get(token,attempts=5):
    for _ in range(attempts):
        found=[n for n in nodes() if token.casefold() in label(n)]
        if found:return found[-1]
        time.sleep(1)
    raise AssertionError("Missing Arena control: "+token)
def bounds(n):
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.attrib.get("bounds",""))
    assert m,n.attrib
    x1,y1,x2,y2=map(int,m.groups());assert x2>x1 and y2>y1
    return x1,y1,x2,y2
def tap(n):
    a,b,c,d=bounds(n)
    adb("shell","input","tap",str((a+c)//2),str((b+d)//2))
def find_arena():
    for _ in range(4):
        ns=nodes()
        exact=[n for n in ns if n.attrib.get("text","").strip().casefold()=="arena"]
        if exact:
            tap(exact[-1]);return
        games=[n for n in ns if "oyunlar" in label(n)]
        if games:
            tap(games[-1]);time.sleep(1)
        else:
            adb("shell","input","swipe","350","1050","350","380","300")
        time.sleep(1)
    raise AssertionError("Could not locate existing Arena route from Home")
def screenshot(name,wide):
    p=OUT/f"arena-{name}.png"
    p.write_bytes(subprocess.check_output(["adb","exec-out","screencap","-p"],timeout=35))
    xml=adb("exec-out","cat","/sdcard/wq121.xml")
    (OUT/f"arena-{name}.xml").write_text(xml)
    img=Image.open(p).convert("RGB")
    w,h=img.size
    assert (w>h)==wide,(w,h)
    # Large white left/right margins were the 1.0.120 regression.
    for x1,x2 in ((.015,.075),(.925,.985)):
        region=img.crop((int(w*x1),int(h*.18),int(w*x2),int(h*.78)))
        pixels=list(region.resize((40,200)).getdata())
        white=sum(min(p)>235 and max(p)-min(p)<18 for p in pixels)/len(pixels)
        assert white<.25,(name,white,"white outer margin")
    print("PASS: full-screen",name,w,h)
def viewport(name,size,density,wide):
    adb("shell","wm","size",size);adb("shell","wm","density",density)
    adb("shell","settings","put","system","accelerometer_rotation","0")
    adb("shell","settings","put","system","user_rotation","0")
    adb("shell","am","force-stop",PKG)
    adb("shell","am","start","-n",PKG+"/com.kelimebulmacasi.a1.MainActivity")
    time.sleep(8)
    find_arena()
    get("Oyun Modları")
    card=[bounds(get(s)) for s in ("Bulmaca Düellosu","Ortak Bulmaca","5 Şıklı Düello")]
    centers=[((x+xx)/2,(y+yy)/2) for x,y,xx,yy in card]
    if wide:
        assert centers[0][0]<centers[1][0]<centers[2][0],centers
        assert max(c[1] for c in centers)-min(c[1] for c in centers)<80,centers
    else:
        assert centers[0][1]<centers[1][1]<centers[2][1],centers
    for label_ in ("Sıralama","Arkadaşlarım","Son Maçlarım","Ayarlar","Bilgi"):
        get(label_)
    screenshot(name,wide)
    # Real controls are not decorative images: open and close actual panels.
    for shortcut,expect in (("Bilgi","Kapat"),("Ayarlar","Arena sesleri"),("Sıralama","Bu cihazda"),("Son Maçlarım","Bu cihazda"),("Arkadaşlarım","Kapat")):
        tap(get(shortcut));get(expect);tap(get("Kapat"));get("Oyun Modları")
    tap(get("Bulmaca Düellosu"));get("Maç Ayarları")
    if os.environ.get("WQ_VERSION") in ("1.0.122","1.0.123"):
        tap(get("Özel"));get("Özel ipucu hakkı")
        field=get("İpucu sayısı");tap(field)
        adb("shell","input","keyevent","KEYCODE_MOVE_END")
        adb("shell","input","keyevent","KEYCODE_DEL")
        adb("shell","input","keyevent","KEYCODE_DEL")
        adb("shell","input","text","7")
        tap(get("Uygula"));get("Özel: 7")
        screenshot(name+"-settings",wide)
        print("PASS: custom hint selection and dark match settings on",name)

    adb("shell","input","keyevent","KEYCODE_BACK");get("Oyun Modları")
    if os.environ.get("WQ_VERSION")=="1.0.123":
        for _ in range(2):
            tap(get("Ortak Bulmaca"));get("Maç Ayarları")
            adb("shell","input","keyevent","KEYCODE_BACK");get("Oyun Modları")
            card=[bounds(get(s)) for s in ("Bulmaca Düellosu","Ortak Bulmaca","5 Şıklı Düello")]
            centers=[((x+xx)/2,(y+yy)/2) for x,y,xx,yy in card]
            if wide:
                assert centers[0][0]<centers[1][0]<centers[2][0],centers
                assert max(c[1] for c in centers)-min(c[1] for c in centers)<80,centers
            else: assert centers[0][1]<centers[1][1]<centers[2][1],centers
        screenshot(name+"-back",wide)
        print("PASS: repeated return keeps the correct full-screen Arena layout",name)

    print("PASS: cards, native settings/back and all shortcuts on",name)
try:
    viewport("phone","1080x2340","420",False)
    viewport("tablet","2560x1600","320",True)
finally:
    adb("shell","wm","size","reset")
    adb("shell","wm","density","reset")
