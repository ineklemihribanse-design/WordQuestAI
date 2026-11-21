#!/usr/bin/env python3
"""Turn the existing real new-game form into a visually rich builder without replacing callbacks."""
from pathlib import Path
import shutil

root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
shutil.copyfile(root/"tools/v175/WordQuest175NewGameHero.kt",
                src/"WordQuest175NewGameHero.kt")
p=src/"NewGameScreen.kt"
s=p.read_text("utf-8")

def replace_once(a,b,label):
    global s
    n=s.count(a)
    assert n==1,(label,n)
    s=s.replace(a,b,1)

replace_once(
    '''    ScreenPage("Yeni Oyun", "Kelime havuzunu ve tahta boyutunu seç", back) {
        BodyText("Seviye")''',
    '''    ScreenPage("Yeni Oyun", "Kelime havuzunu ve tahta boyutunu seç", back) {
        Wq175NewGameHero(level,session.profileKey,available)
        BodyText("Seviye")''',
    "premium hero")
replace_once(
    '''        PrimaryAction(if (loading) "Bulmaca hazırlanıyor…" else "Bulmacayı başlat", {''',
    '''        Wq175NewGameSummary(level,topic,kind,count,available,timedMinutes)
        PrimaryAction(if (loading) "Bulmaca hazırlanıyor…" else "Bulmacayı başlat", {''',
    "dynamic game summary")
p.write_text(s,"utf-8")
print("PASS: real new-game controls now have a mascot/board hero and accurate live selection summary.")
