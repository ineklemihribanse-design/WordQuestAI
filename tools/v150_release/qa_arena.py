"""Check actual v150 guest-safe Arena navigation, native controls and display bounds."""
import os,re,time,subprocess,xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
PKG="com.kelimebulmacasi.a1.practice.fresh"
OUT=Path(os.environ["WQ_RELEASE_DIR"])
def adb(*a):
    return subprocess.check_output(["adb",*a],stderr=subprocess.STDOUT,timeout=45).decode("utf-8","replace")
def nodes():
    adb("shell","uiautomator","dump","/sdcard/wq150.xml")
    return list(ET.fromstring(adb("exec-out","cat","/sdcard/wq150.xml")).iter("node"))
def label(n):return " ".join((n.get("text",""),n.get("content-desc",""))).strip().casefold()
def bounds(n):
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.get("bounds",""))
    assert m,n.attrib
    x,y,X,Y=map(int,m.groups());assert X>x and Y>y,("Clipped accessibility node",n.attrib)
    return x,y,X,Y
def get(token):
    for _ in range(6):
        found=[n for n in nodes() if token.casefold() in label(n)]
        if found:return next((n for n in found if n.get("clickable")=="true"),found[-1])
        time.sleep(1)
    raise AssertionError("Missing: "+token)
def tap(n):
    x,y,X,Y=bounds(n);adb("shell","input","tap",str((x+X)//2),str((y+Y)//2))
def capture(name):
    p=OUT/f"arena-{name}.png"
    p.write_bytes(subprocess.check_output(["adb","exec-out","screencap","-p"],timeout=35))
    (OUT/f"arena-{name}.xml").write_text(adb("exec-out","cat","/sdcard/wq150.xml"))
    im=Image.open(p).convert("RGB");assert im.width>0 and im.height>0
    return im.size
def enter(w,h):
    last=""
    launcher_recovered=False
    for i in range(16):
        ns=nodes();ls=[label(n) for n in ns]
        # The disposable emulator's own launcher may ANR after a display resize.
        # Recover ONLY that external system dialog; an app ANR must still fail.
        launcher_anr=any(n.get("package")=="android" and
            "pixel launcher isn't responding" in n.get("text","").casefold() for n in ns)
        if launcher_anr:
            if launcher_recovered:
                raise AssertionError("Pixel Launcher ANR persisted after recovery")
            close=next((n for n in ns if n.get("resource-id")=="android:id/aerr_close"),None)
            assert close is not None,"Pixel Launcher ANR has no close button"
            print("Recovering external Pixel Launcher ANR, not WordQuest",flush=True)
            tap(close)
            adb("shell","am","start","-n",PKG+"/com.kelimebulmacasi.a1.MainActivity")
            launcher_recovered=True
            time.sleep(1)
            continue
        if any("bulmaca düellosu" in l for l in ls) and any("tek başına oyna" in l for l in ls):return
        def pick(fn):
            a=[n for n in ns if fn(label(n))]
            return next((n for n in a if n.get("clickable")=="true"),a[-1] if a else None)
        e=pick(lambda l:re.search(r"arena[’']?ya\s+gir",l) is not None)
        c=pick(lambda l:l=="arena" or l.startswith("arena düello") or l.startswith("arena, düello"))
        d=pick(lambda l:l in ("daha sonra","şimdi değil","kapat","vazgeç"))
        if e is not None:tap(e);last="entry"
        elif c is not None and last!="category":tap(c);last="category"
        elif d is not None:tap(d)
        else:adb("shell","input","swipe",str(w//2),str(int(h*(.78 if i%8<4 else .25))),str(w//2),str(int(h*(.25 if i%8<4 else .78))),"350")
        time.sleep(1.5)
    raise AssertionError("Arena entry not reached")
def viewport(name,w,h,density,wide):
    adb("shell","wm","size",f"{w}x{h}");adb("shell","wm","density",str(density))
    adb("shell","settings","put","system","accelerometer_rotation","0")
    adb("shell","settings","put","system","user_rotation","0")
    adb("shell","am","force-stop",PKG);adb("shell","am","start","-n",PKG+"/com.kelimebulmacasi.a1.MainActivity");time.sleep(8)
    enter(w,h)
    assert not any("kişisel maskot" in label(n) for n in nodes())
    # One stable accessibility snapshot instead of nine expensive UIAutomator dumps.
    initial=nodes()
    def initial_bounds(token):
        matches=[n for n in initial if token.casefold() in label(n)]
        if not matches:return bounds(get(token))
        return bounds(next((n for n in matches if n.get("clickable")=="true"),matches[-1]))
    cards=[initial_bounds(t) for t in ("Bulmaca Düellosu","Ortak Bulmaca","5 Şıklı Düello","Takım Oyunu")]
    footer=[initial_bounds(t) for t in ("Sıralama","Arkadaşlarım","Son Maçlarım","Ayarlar","Bilgi")]
    print("Arena card and footer coordinates",name,cards,footer,flush=True)
    W,H=capture(name+"-online")
    assert (W>H)==wide
    assert all(0<=x<X<=W and 0<=y<Y<=H for x,y,X,Y in cards+footer)
    assert max(a[3] for a in cards)<=min(a[1] for a in footer)
    if wide:assert all(a[0]<b[0] for a,b in zip(cards,cards[1:]))
    else:assert all(a[1]<b[1] for a,b in zip(cards,cards[1:]))
    tap(get("Tek Başına Oyna"));get("Klasik Bulmaca");get("Engelli Parkur");capture(name+"-solo")
    assert not any("takım oyunu" in label(n) for n in nodes())
    tap(get("Klasik Bulmaca"));get("Maç Ayarları")
    # Match actual labels, not the same text in the sticky match summary.
    level_names=("A1","A2","B1","B2","C1","C2")
    def exact_levels():
        ns=nodes()
        result=[]
        for token in level_names:
            matches=[n for n in ns if n.get("text","").strip()==token]
            assert len(matches)==1,(token,len(matches))
            result.append(matches[0])
        return result
    buttons=exact_levels()
    levels=[bounds(n) for n in buttons]
    assert max(a[1] for a in levels)-min(a[1] for a in levels)<=8,levels
    assert all(a[0]<b[0] for a,b in zip(levels,levels[1:])),levels
    tap(buttons[1])
    time.sleep(0.6)
    assert any("a1 + a2" in label(n) for n in nodes()),"A2 selection missing from match summary"
    tap(exact_levels()[1])
    time.sleep(0.6)
    assert not any("a1 + a2" in label(n) for n in nodes()),"A2 deselection missing from match summary"
    get("Oyunu Başlat")
    # Compose exposes Button and its Text as separate accessibility nodes.
    # Follow the actual label's ancestry; never accept an unrelated button.
    root=ET.fromstring(adb("exec-out","cat","/sdcard/wq150.xml"))
    parents={child:parent for parent in root.iter() for child in parent}
    matches=[n for n in root.iter("node") if n.get("text","").strip()=="Oyunu Başlat"]
    assert len(matches)==1,("Start label count",len(matches))
    title=matches[0]
    start=title
    while start is not None and start.get("clickable")!="true":
        start=parents.get(start)
    assert start is not None,("Start label has no clickable ancestor",title.attrib)
    assert start.get("enabled")=="true" and title.get("enabled")=="true",start.attrib
    x,y,X,Y=bounds(start)
    tx,ty,TX,TY=bounds(title)
    assert 0<=x<=tx<TX<=X<=W and 0<=y<=ty<TY<=Y<=H,("Start button outside display",start.attrib,title.attrib)
    assert Y-y>=int(44*density/160),("Start touch target too small",start.attrib)
    assert Y<=min(a[1] for a in levels),("Start must remain above scrollable level controls",start.attrib,levels)
    print("PASS: visible enabled start button and its text",name,start.attrib,flush=True)
    capture(name+"-settings")
    adb("shell","input","keyevent","KEYCODE_BACK");get("Klasik Bulmaca")
    print("PASS: Arena mode tabs, footer, single-row levels and solo settings",name,flush=True)
try:
    viewport("phone",1080,2340,420,False)
    viewport("tablet",2560,1600,320,True)
    viewport("tablet-short",1724,1018,240,True)
except Exception:
    capture("failure")
    raise
finally:
    adb("shell","wm","size","reset");adb("shell","wm","density","reset")
