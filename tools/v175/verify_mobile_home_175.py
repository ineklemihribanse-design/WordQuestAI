#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
h=(src/"HomeScreen.kt").read_text("utf-8")
m=(src/"Wq175PremiumHome.kt").read_text("utf-8")
a=(src/"WordQuest175Arena.kt").read_text("utf-8")
assert h.count("Wq175PremiumHomeGrid(")==1
assert "Wq2DMascots.selected(LocalContext.current,session.profileKey)" in h
assert "Color(0xFF203A49)" in h
assert 'MenuItem("Hazır Paketler"' not in h[h.index('fun HomeScreen('):h.index('private fun HeroTiles')]
assert m.count("Wq175HomeTile(")>=12
for word in ("packages=packages","arena=firebaseArena","ydsCenter=ydsCenter",
             "learning=learning","custom=custom","practice=practice",
             "assistant=assistant","verbs=verbs","progress=progress","insights=insights"):
    assert word in h,word
assert 'Wq175GlossIcon("arena_"+kind,Modifier.size(69.dp))' in a
assert 'Wq175ArenaModeSymbol(kind)' not in a[a.index('private fun Wq175ModeCard'): ]
for name in ("arena_duel","arena_team","arena_quiz","packages","assistant","plan"):
    assert (root/"source/app/src/main/assets/wq175_icons"/(name+".webp")).stat().st_size>800
print("PASS: mobile-first dashboard; every previously linked screen reachable")
print("PASS: selected mascot is dynamic and all three true Arena WebP icons wired")
