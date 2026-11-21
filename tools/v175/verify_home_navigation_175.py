#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
s=(src/"HomeScreen.kt").read_text("utf-8")
nav=(src/"Wq175HomeNavigation.kt").read_text("utf-8")
assert 'Wq175HomeBottomNav(' in s
assert 'session=session,onNewGame=newGame,onArena=firebaseArena' in s
assert 'onDaily=practice,onAccount=account' in s
assert 'Spacer(Modifier.height(90.dp))' in s
assert 'Wq175HomeAlerts(' in s
assert 'due=session.learningDueCount()' in s
assert 'NavigationBarItem(selected=true,enabled=false' in nav
assert nav.count('NavigationBarItem(')==5
for slug in ("ana_sayfa","yeni_oyun","arena","gunluk_plan"):
    assert 'Wq175GlossIcon("'+{"ana_sayfa":"puzzle","yeni_oyun":"puzzle","arena":"trophy","gunluk_plan":"plan"}[slug]+'",Modifier.size(29.dp))' in nav
for label in ('"Yeni Oyun"','"Arena"','"Günlük"','"Profil"'):
    assert label in nav,label
assert 'onClick={expanded=false;onMemory()}' in nav
assert 'onClick={expanded=false;onDaily()}' in nav
ET.parse(root/"source/app/src/main/res/drawable/wq175_home_alert_bell.xml")
print("PASS: fixed app-like bottom bar, real 4 actions, due-alert menu and safe content scrolling.")
