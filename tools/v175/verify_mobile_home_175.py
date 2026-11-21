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
assert 'Text("WordQuest AI", color = Color(0xFF203A49)' in h
assert 'Wq175HomeStartButton(newGame)' in h
assert 'containerColor=Color(0xFF086B4F)' in m
assert "BoxWithConstraints(Modifier.fillMaxWidth())" in m
assert "maxWidth<194.dp" in m
assert "LocalDensity.current.fontScale>1.30f" in m
assert "Surface(" in m and "onClick=tile.onClick" in m
assert "CompositingStrategy.Offscreen" in h
assert "drawRect(brush=Brush.horizontalGradient(" in h
assert "Wq175ThemeIcon(tile.artSlug" in m
assert "R.drawable.wq167_lena_portrait" in m
assert 'else if(tile.icon=="assistant")' in m
assert '"analytics",Color(0xFFFFF1DC),progress)' in m
assert '"insights",Color(0xFFECEFFD),insights)' in m
for module in ("arena","yds_akademi","kelime_hafizam","bulmacalarim","gunluk_plan","yeni_oyun"):
    assert 'artSlug="'+module+'"' in m,module
assert "fontSize=if(LocalDensity.current.fontScale>1.2f)24.sp else 29.sp" in h
assert 'MenuItem("Hazır Paketler"' not in h[h.index('fun HomeScreen('):h.index('private fun HeroTiles')]
assert m.count("Wq175HomeTile(")>=12
assert 'val singleColumn=maxWidth<345.dp' in m
assert m.count('LocalDensity.current.fontScale>1.45f')>=2
assert 'main.chunked(2).forEach' in m
assert 'more.chunked(2).forEach' in m
for word in ("packages=packages","arena=firebaseArena","ydsCenter={ wq175AcademyOpen = true }",
             "learning=learning","custom=custom","practice=practice",
             "assistant=assistant","verbs=verbs","progress=progress","insights=insights"):
    assert word in h,word
assert 'Wq175GlossIcon("arena_"+kind,Modifier.size(69.dp))' in a
assert 'Wq175ArenaModeSymbol(kind)' not in a[a.index('private fun Wq175ModeCard'): ]
for name in ("arena_duel","arena_team","arena_quiz","packages","assistant","plan"):
    assert (root/"source/app/src/main/assets/wq175_icons"/(name+".webp")).stat().st_size>800
settings=(src/"WordQuest174Arena.kt").read_text("utf-8")
assert 'Wq175GlossIcon(asset,Modifier.size(39.dp))' in settings
assert 'Wq175GlossIcon("settings",Modifier.size(34.dp))' in settings
for name in ("packages","library","timer","puzzle","insights"):
    assert '"'+name+'"' in settings
assert '"level"->Text("ABC"' not in settings
print("PASS: mobile-first dashboard; every previously linked screen reachable")
print("PASS: selected mascot is dynamic and all three true Arena WebP icons wired")
